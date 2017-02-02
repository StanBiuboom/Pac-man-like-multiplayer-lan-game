
// The client part of our game: UmiClient.java
// method we used£ºjava UmiClient
// when the program started, press login button, and the server name and the player's name will be asked
// to terminate the program, press logout

import java.awt.*;// graphics library
import java.awt.event.*;// event-related library
import java.net.*;// net-related library
import java.io.*;
import java.util.*;
import javax.swing.JOptionPane;

// UmiClient class
// the main part of UmiClient
public class UmiClient implements Runnable {
	Frame f;// the window to show client info
	Panel p;// the panel to show up, down, left,right buttons and the status of
			// the sea
	Canvas c;// generate the Canvas to show the sea

	// constructor
	// initial setting up with GUI
	public UmiClient() {
		Button b;
		f = new Frame();// to show the window of the entire client info
		p = new Panel();// to show the buttons and the sea
		p.setLayout(new BorderLayout());

		
		b = new Button("up");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sendCommand("up");
			}
		});
		p.add(b, BorderLayout.NORTH);

		b = new Button("left");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sendCommand("left");
			}
		});
		p.add(b, BorderLayout.WEST);

		b = new Button("right");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sendCommand("right");
			}
		});
		p.add(b, BorderLayout.EAST);

		b = new Button("down");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sendCommand("down");
			}
		});
		p.add(b, BorderLayout.SOUTH);

		// generate the Canvas to show the sea
		c = new Canvas();
		c.setSize(256, 256);// set the size of the sea
		// set the GUI widgets
		p.add(c);
		f.add(p);

		// add login button on frame f
		b = new Button("login");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// press login button...then... 
				// if the server hasn't been set up, then execute the handling part
				if (server == null)
					login();
			}
		});
		f.add(b, BorderLayout.NORTH);

		b = new Button("logout");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				logout();
			}
		});
		f.add(b, BorderLayout.SOUTH);

		// to show frame f
		f.setSize(335, 345);
		f.show();
	}

	// run method
	// refresh the frame every 750 ms
	public void run() {
		while (true) {
			try {
				Thread.sleep(750);
			} catch (Exception e) {
			}
			// use the repain method to print the info coming from the server on the screen
			repaint();
		}
	}

	// object related to login method
	int sx = 100;
	int sy = 100;
	TextField host, tf_name;
	Dialog d;

	// login method
	// to show the login window and to obtain the info needed
	// the actual login logic is implemented in the realLogin method
	void login() {
		// to show the window and the data input
		d = new Dialog(f, true);
		host = new TextField(10);
		tf_name = new TextField(10);
		d.setLayout(new GridLayout(3, 2));
		d.add(new Label("host:"));
		d.add(host);
		d.add(new Label("name:"));
		d.add(tf_name);
		Button b = new Button("OK");
		b.addActionListener(new ActionListener() {
			// if the input is ended, then use the realLogin method to login the server
			public void actionPerformed(ActionEvent e) {
				realLogin(host.getText(), tf_name.getText());
				d.dispose();
			}
		});
		d.add(b);
		d.setResizable(true);
		d.setSize(200, 150);
		d.show();
		(new Thread(this)).start();
	}

	// objects related to realLogin
	Socket server;// socket which used to link with the server
	int port = 10000;// port for connection
	BufferedReader in;// I/O
	PrintWriter out;
	String name;// player's name

	// realLogin method
	void realLogin(String host, String name) {
		try {
			// connect to the server
			this.name = name;
			server = new Socket(host, port);
			in = new BufferedReader(new InputStreamReader(server.getInputStream()));
			out = new PrintWriter(server.getOutputStream());

			// sending login cmd
			out.println("login " + name);
			out.flush();
			repaint();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	// logout method
	void logout() {
		try {
			// sending logout cmd
			out.println("logout");
			out.flush();
			server.close();
		} catch (Exception e) {
			;
		}
		System.exit(0);
	}

	// repaint method
	// get info from the server and refresh the client's UI
	void repaint() {
		// send STAT cmd to the server and get the status info about the sea
		out.println("stat");
		out.flush();
		if(!server.isConnected()){
			out.println("logout");
			out.flush();
		}
			
		try {
			String line = in.readLine();// read the data from the server
			Graphics g = c.getGraphics();// draw the sea on c

			// drawing the sea
			g.setColor(Color.blue);
			g.fillRect(0, 0, 256, 256);

			// to search the info about ships from "ship_info"
			while (!"ship_info".equalsIgnoreCase(line)) {
				// to judge whether the winning or losing condition has been hit
				if ("win".equalsIgnoreCase(line)) { // winning
					d = new Dialog(f, true);
					d.setTitle("You Won");
					d.add(new Label("Congratulations :)"));
					d.setLayout(new GridLayout(3, 2));
					Button b = new Button("OK");
					b.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							logout();
							d.dispose();
						}
					});
					d.add(b);
					d.setResizable(true);
					d.setSize(200, 150);
					d.show();
				}
				if ("lose".equalsIgnoreCase(line)) { //losing
					d = new Dialog(f, true);
					d.setTitle("Game Over");
					d.add(new Label("You have lost the game :("));
					d.setLayout(new GridLayout(3, 2));
					Button b = new Button("OK");
					b.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							logout();
							d.dispose();
						}
					});
					d.add(b);
					d.setResizable(true);
					d.setSize(200, 150);
					d.show();
				}
				line = in.readLine();
			}
			// to show the info about the ships in ship_info
			// ship_info info will be separated by '.'
			line = in.readLine();
			while (!".".equals(line)) {
				StringTokenizer st = new StringTokenizer(line);
				// read the player's name
				String obj_name = st.nextToken().trim();

				// use red spot to show the player's own ship and use green spots to show other players' ships
				if (obj_name.equals(name)) // show the player's own ship
					g.setColor(Color.red);
				else // show other players' ships
					g.setColor(Color.green);

				// read the coordinate of the ship
				int x = Integer.parseInt(st.nextToken());
				int y = Integer.parseInt(st.nextToken());
				String life = st.nextToken();
				int flag = Integer.parseInt(life);
				if (flag == 0 && obj_name.equals(name)) {
					d = new Dialog(f, true);
					d.setTitle("Game Over");
					d.add(new Label("You have lost the game!"));
					d.setLayout(new GridLayout(3, 2));
					Button b = new Button("OK");
					b.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							logout();
							d.dispose();
						}
					});
					d.add(b);
					d.setResizable(true);
					d.setSize(200, 150);
					d.show();
				}

				else {
					// show the ship
					g.fillOval(x - 10, 256 - y - 10, 20, 20);
					// on the lower right corner, show the score
					g.drawString(life, x + 10, 256 - y + 10);
					// on the upper right corner, show the player's name
					g.drawString(obj_name, x + 10, 256 - y - 10);
				}
				line = in.readLine();
			}

			// wait for receiving the info for the energy barrels, starting from "energy_info" 
			while (!"energy_info".equalsIgnoreCase(line))
				line = in.readLine();

			// show energy barrel info through energy_info
			// energy_info is separated by '.'
			line = in.readLine();
			while (!".".equals(line)) {
				StringTokenizer st = new StringTokenizer(line);
				// read the coordinator of the barrel
				int x = Integer.parseInt(st.nextToken());
				int y = Integer.parseInt(st.nextToken());
				int z = Integer.parseInt(st.nextToken());
				if (z == 0 ||z == 1) {
					g.setColor(Color.yellow);
					g.fillOval(x - 5, 256 - y - 5, 10, 10);
				} else {
					g.setColor(Color.black);
					g.fillOval(x - 5, 256 - y - 5, 10, 10);
				}
				// read next line
				line = in.readLine();
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	// sendCommand method
	// send cmds to the server
	void sendCommand(String s) {
		if ("up".equals(s)) {
			out.println("up");
		} else if ("down".equals(s)) {
			out.println("down");
		} else if ("left".equals(s)) {
			out.println("left");
		} else if ("right".equals(s)) {
			out.println("right");
		}
		// out.println("stat");
		out.flush();
	}

	// main method
	// start UmiClient
	public static void main(String[] arg) {
		new UmiClient();
	}
}