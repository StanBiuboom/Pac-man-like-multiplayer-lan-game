
// The server part for our game,UmiServer.java
// method we used:java UmiServer
// when the program starts£¬it will wait for the linking request from clients through port 10000
// to terminate the program, press<Ctrl>+C

import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.plaf.synth.SynthDesktopIconUI;

// UmiServer class
// UmiServer class is the main part of UmiServer
public class UmiServer {
	static final int DEFAULT_PORT = 10000;
	// UmiServer port number
	static ServerSocket serverSocket;
	static Vector connections;
	// this vector is responsible for maintaining the clients connection
	static Vector energy_v; // storing the info for all the energy barrels
	static Hashtable<String, Ship> userTable = null;
	// login table storing info for all the clients
	static Random random = null;

	public static HashFunction hashFunction = new HashFunction();
	private static int NUMBEROFTASK = 100;
	private static ConsistentHash consistentHash = new ConsistentHash(hashFunction, 1, NUMBEROFTASK);

	// the consistenthash entity is the main part for the implementation of our
	// algorithm
	public static Hashtable getUserTable() {
		return userTable;
	}

	// addConnection method
	// for adding all the login client info into the connections vector
	public static void addConnection(Socket s) {
		if (connections == null) {// initialization...
			connections = new Vector();// generate connections
		}
		connections.addElement(s);
	}

	// deleteConnection method
	// for deleting the login client info from the connections vector
	public static void deleteConnection(Socket s) {
		if (connections != null) {
			connections.removeElement(s);
		}
	}

	// loginUser method
	// for storing the name of the client and the ship info
	public static void loginUser(String name) {
		if (userTable == null) {// create the usertable if it does not exist yet
			userTable = new Hashtable();
		}
		if (random == null) {// preparing for the random number
			random = new Random();
		}

		// use the random number to locate the initial location of the ship
		int ix = Math.abs(random.nextInt()) % 256;
		int iy = Math.abs(random.nextInt()) % 256;

		// storing the name and the ship entity
		Ship newShip = new Ship(ix, iy, name, consistentHash, userTable);
		newShip.refreshPoint();
		userTable.put(name, newShip);

		// print out the user name on the screen of the server
		System.out.println("login:" + name);
		System.out.flush();
		// for the changed time vector algorithm
		refreshIndex();
		addOneElementToVectors();
	}

	// refresh the index of all the players in the userTable entity
	public static void refreshIndex() {
		for (Enumeration users = userTable.keys(); users.hasMoreElements();) {
			String user = users.nextElement().toString();
			Ship ship = (Ship) userTable.get(user);
			int i = ship.updateIndex();
			System.out.println("name: " + ship.name + " index: " + i);
		}
	}

	public static int getMaxVectorSize() {
		int maxSize = 0;
		for (Enumeration users = userTable.keys(); users.hasMoreElements();) {
			String user = users.nextElement().toString();
			Ship ship = (Ship) userTable.get(user);
			int vectorSize = ship.getVectorSize();
			if (vectorSize > maxSize) {
				maxSize = vectorSize;
			}
		}
		return maxSize;
	}

	// if a new user login the game, the length of the vectors should be updated
	public static void addOneElementToVectors() {
		int maxSize = getMaxVectorSize();
		for (Enumeration users = userTable.keys(); users.hasMoreElements();) {
			String user = users.nextElement().toString();
			Ship ship = (Ship) userTable.get(user);
			ship.addOneElementToVector();
		}

		for (Enumeration users = userTable.keys(); users.hasMoreElements();) {
			String user = users.nextElement().toString();
			Ship ship = (Ship) userTable.get(user);
			for (int i = ship.getVectorSize() - 1; i < maxSize; i++) {
				ship.addOneElementToVector();
			}
			ship.printVectors();
		}

	}

	public static int getCurrentIndex(String name) {
		int currentIndex = 0;
		for (Enumeration users = userTable.keys(); users.hasMoreElements();) {
			String user = users.nextElement().toString();
			if (user.equals(name)) {
				return currentIndex;
			} else {
				currentIndex++;
			}
		}
		return -1;
	}

	// if a user logout the game, also, the length of the vectors should be
	// updated
	public static void reduceOneElementToVectors(int indexToDelete) {
		for (Enumeration users = userTable.keys(); users.hasMoreElements();) {
			String user = users.nextElement().toString();
			Ship ship = (Ship) userTable.get(user);
			ship.reduceOneElementToVector(indexToDelete);
			ship.printVectors();
		}
	}

	// logoutUser method
	public static void logoutUser(String name) {
		// print out the user name on the screen of the server
		System.out.println("logout:" + name);
		System.out.flush();
		// delete the player in relevant tables
		int currentIndex = getCurrentIndex(name);
		reduceOneElementToVectors(currentIndex);
		System.out.println("index before logging out£º " + currentIndex);
		userTable.remove(name);
		consistentHash.removeHashNode(name);
		// refresh the scores of all the players
		for (Enumeration users = userTable.keys(); users.hasMoreElements();) {
			String user = users.nextElement().toString();
			Ship ship = (Ship) userTable.get(user);
			ship.refreshPoint();
		}
		refreshIndex();
	}

	// left method
	// move the specific ship to its left and judge whether it hits an energy
	// barrel
	// use calculation method for judging
	public static void left(String name) {
		Ship ship = (Ship) userTable.get(name);
		ship.left();
		calculation();
	}

	// right method
	// move the specific ship to its right and judge whether it hits an energy
	// barrel
	// use calculation method for judging
	public static void right(String name) {
		Ship ship = (Ship) userTable.get(name);
		ship.right();
		calculation();
	}

	// up method
	// move the specific ship to its up position and judge whether it hits an
	// energy barrel
	// use calculation method for judging
	public static void up(String name) {
		Ship ship = (Ship) userTable.get(name);
		ship.up();
		calculation();
	}

	// down method
	// move the specific ship to its down position and judge whether it hits an
	// energy barrel
	// use calculation method for judging
	public static void down(String name) {
		Ship ship = (Ship) userTable.get(name);
		ship.down();
		calculation();
	}

	// calculation method
	// to figure out the location relationships between a ship and all the
	// energy barrels and judge whether the ship has hit one barrel
	static void calculation() {
		if (userTable != null && energy_v != null) {
			// judging all the players
			for (Enumeration users = userTable.keys(); users.hasMoreElements();) {
				// loading the all the players' names and the location info for
				// all the ships
				String user = users.nextElement().toString();
				Ship ship = (Ship) userTable.get(user);
				// figure out the relationships between a ship and all the
				// energy barrels
				for (Enumeration energys = energy_v.elements(); energys.hasMoreElements();) {
					// get the info of all the barrels
					int[] e = (int[]) energys.nextElement();
					int x = e[0] - ship.x;
					int y = e[1] - ship.y;
					int z = e[2]; // this helps to judge whether its a yellow
									// one or a black one
					double r = Math.sqrt(x * x + y * y);
					// if the distance is within 10, then the ship is regarded
					// as picking up the barrel
					if (r < 10) {
						energy_v.removeElement(e);
						if (e[2] == 0 || e[2] == 1) {// yellow barrel
							consistentHash.addOneTask(user);
							ship.updateAdditionVector();
							System.out.println("Before merge: "); // for showing
																	// the
																	// results
																	// before
																	// and after
																	// using the
																	// time
																	// vector
																	// algorithm
							printAllUsersVectors();
							mergeVectors();
							System.out.println("After merge: ");
							printAllUsersVectors();
						} else {// black barrel
							consistentHash.reduceOneTask(user);
							ship.updateDedutionVector();
							System.out.println("Before merge: ");
							printAllUsersVectors();
							mergeVectors();
							System.out.println("After merge: ");
							printAllUsersVectors();
						}
					}
				}
			}
		}
	}

	public static void printAllUsersVectors() {
		for (Enumeration users = userTable.keys(); users.hasMoreElements();) {
			String user = users.nextElement().toString();
			Ship ship = (Ship) userTable.get(user);
			ship.printVectors();
		}
	}

	public static void mergeVectors() {
		Enumeration<String> keys = userTable.keys();
		String firstName = keys.nextElement();
		Ship firstShip = (Ship) userTable.get(firstName);
		int vectorSize = firstShip.getVectorSize();
		for (int i = 0; i < vectorSize; i++) {
			int maxInAddition = 0;
			int maxInDeduction = 0;
			// addition
			for (Enumeration users = userTable.keys(); users.hasMoreElements();) {
				String user = users.nextElement().toString();
				Ship ship = (Ship) userTable.get(user);
				int value1 = ship.getValueInVectorByIndex("addition", i);
				if (value1 > maxInAddition)
					maxInAddition = value1;
			}
			for (Enumeration users = userTable.keys(); users.hasMoreElements();) {
				String user = users.nextElement().toString();
				Ship ship = (Ship) userTable.get(user);
				ship.setVectorValueByIndex("addition", maxInAddition, i);
			}
			// deduction
			for (Enumeration users = userTable.keys(); users.hasMoreElements();) {
				String user = users.nextElement().toString();
				Ship ship = (Ship) userTable.get(user);
				int value2 = ship.getValueInVectorByIndex("deduction", i);
				if (value2 > maxInDeduction)
					maxInDeduction = value2;
			}
			for (Enumeration users = userTable.keys(); users.hasMoreElements();) {
				String user = users.nextElement().toString();
				Ship ship = (Ship) userTable.get(user);
				ship.setVectorValueByIndex("deduction", maxInDeduction, i);
			}
		}

		// int additionSum = firstShip.getSumOfVector("addition");
		// int deductionSum = firstShip.getSumOfVector("deduction");
		// int numberOfTask = consistentHash.getNumberOfTask();

	}

	// for judging whether the players have touched the winning condition
	public static boolean isWon() {
		Enumeration<String> keys = userTable.keys();
		String firstName = keys.nextElement();
		Ship firstShip = (Ship) userTable.get(firstName);
		int numberOfTask = consistentHash.getNumberOfTask();
		int additionSum = firstShip.getSumOfVector("addition");
		int deductionSum = firstShip.getSumOfVector("deduction");
		// System.out.println("current score difference£ºa-d: " + (additionSum -
		// deductionSum));
		if ((additionSum - deductionSum) > 10) {
			return true;
		} else {
			return false;
		}
	}

	// for judging whether the players have touched the losing condition
	public static boolean isLost() {
		Enumeration<String> keys = userTable.keys();
		String firstName = keys.nextElement();
		Ship firstShip = (Ship) userTable.get(firstName);
		int numberOfTask = consistentHash.getNumberOfTask();
		int additionSum = firstShip.getSumOfVector("addition");
		int deductionSum = firstShip.getSumOfVector("deduction");
		// System.out.println("current score difference:d-a: " + (deductionSum -
		// additionSum));
		if ((deductionSum - additionSum) > 10) {
			return true;
		} else {
			return false;
		}
	}

	// statInfo method
	// handle with STAT cmd
	// sending ship_info to clients
	// and sending energy_info to the clients
	public static void statInfo(PrintWriter pw) {
		try {
			if (isWon()) {
				pw.println("win");
				pw.println(".");
			}
			if (isLost()) {
				pw.println("lose");
				pw.println(".");
			}
		} catch (Exception e) {
		}
		// sending ship info:(ship_info)
		int mark = 0;
		pw.println("ship_info");
		if (userTable != null) {
			for (Enumeration users = userTable.keys(); users.hasMoreElements();) {
				String user = users.nextElement().toString();
				Ship ship = (Ship) userTable.get(user);
				ship.refreshPoint();
				pw.println(user + " " + ship.x + " " + ship.y + " " + ship.point);
			}
		}
		pw.println(".");// ship_info ends
		// sending energy info (energy_info£©
		pw.println("energy_info");
		if (energy_v != null) {
			// sending the info of all the energy_info to all the clients
			for (Enumeration energys = energy_v.elements(); energys.hasMoreElements();) {
				int[] e = (int[]) energys.nextElement();
				pw.println(e[0] + " " + e[1] + " " + e[2]);
				if (e[2] == 0 || e[2] == 1) // yellow barrel
					e[0] = (e[0] + 15) % 256;
				else
					e[0] = (e[0] + 10) % 256;
				energy_v.setElementAt(e, mark++);
			}
		}
		pw.println(".");// enegy_info ends
		pw.flush();
	}

	// putEnergy method
	// randomly put an energy barrel on the screen
	public static void putEnergy() {
		if (energy_v == null) {// initialization...
			energy_v = new Vector();
		}
		if (random == null) {// using random number
			random = new Random();
		}
		// use the random number to locate the initial location of the barrels
		int[] e = new int[3];
		e[0] = Math.abs(random.nextInt()) % 256;
		e[1] = Math.abs(random.nextInt()) % 256;
		e[2] = Math.abs(random.nextInt()) % 4;
		energy_v.addElement(e);
	}

	// main method
	// generating the server socket, handling with the connection with the
	// clients and accomplishing the energy barrels initialization in proper
	// time.
	public static void main(String[] arg) {
		try {// generating the server socket
			serverSocket = new ServerSocket(DEFAULT_PORT);
		} catch (IOException e) {
			System.err.println("can't create server socket.");
			System.exit(1);
		}

		// generating the thread et for adding the barrels
		Thread et = new Thread() {
			public void run() {
				while (true) {
					try {
						sleep(3000);
					} catch (InterruptedException e) {
						break;
					}
					// add a barrel on the screen
					UmiServer.putEnergy();
				}
			}
		};
		// start et
		et.start();
		// taking the incoming client sockets and handling with the user cmds
		while (true) {
			try {
				Socket cs = serverSocket.accept();
				addConnection(cs);// login connection
				// generating the clientPro thread
				Thread ct = new Thread(new clientProc(cs));
				ct.start();
			} catch (IOException e) {
				System.err.println("client socket or accept error.");
			}
		}
	}
}

// clientProc class
// clientProc class is used for handling with the clients sockets
class clientProc implements Runnable {
	Socket s; // the socket for clients to connect
	// I/O
	BufferedReader in;
	PrintWriter out;
	String name = null;// client name

	// constructor of clientProc
	public clientProc(Socket s) throws IOException {
		this.s = s;
		in = new BufferedReader(new InputStreamReader(s.getInputStream()));
		out = new PrintWriter(s.getOutputStream());
	}

	// run method
	// the main part of the client thread
	public void run() {
		try {
				while (true) {
					if(out.checkError()){
						UmiServer.logoutUser(name);
						UmiServer.deleteConnection(s);
						s.close();
					}
					else{
						//System.out.println("1111");
					}
					// keep waiting for the LOGOUT cmd
					String line = in.readLine();
					// when the name is null, only LOGIN cmd will be answered
					if (name == null) {
						StringTokenizer st = new StringTokenizer(line);
						String cmd = st.nextToken();
						if ("login".equalsIgnoreCase(cmd)) {
							name = st.nextToken();
							UmiServer.loginUser(name);
							// UmiServer.statInfo(out);
						} else {
							// all the other cmds except LOGIN will be ignored
						}
					} else {
						UmiServer.calculation();
						for (Enumeration users = UmiServer.userTable.keys(); users.hasMoreElements();) {
							String user = users.nextElement().toString();
							Ship ship = (Ship) UmiServer.userTable.get(user);
							ship.refreshPoint();
						}

						// because name is not null, the LOGOUT cmd has been
						// handled
						// with, so starts to accept other cmds
						StringTokenizer st = new StringTokenizer(line);
						String cmd = st.nextToken();// reading cmds
						// parse the cmds and deal with them
						if ("STAT".equalsIgnoreCase(cmd)) {
							UmiServer.statInfo(out);
						} else if ("UP".equalsIgnoreCase(cmd)) {
							UmiServer.up(name);
						} else if ("DOWN".equalsIgnoreCase(cmd)) {
							UmiServer.down(name);
						} else if ("LEFT".equalsIgnoreCase(cmd)) {
							UmiServer.left(name);
						} else if ("RIGHT".equalsIgnoreCase(cmd)) {
							UmiServer.right(name);
						} else if ("LOGOUT".equalsIgnoreCase(cmd)) {
							UmiServer.logoutUser(name);
							UmiServer.statInfo(out);
							break;
						}
					}
				}
				// delete the login info, break the connection
				UmiServer.deleteConnection(s);
				// UmiServer.logoutUser(name);
				// s.close();
		} catch (IOException e) {
			try {
				UmiServer.logoutUser(name);
				s.close();
			} catch (IOException e2) {
			}
		}
	}
}

// Ship class
class Ship {
	// coordinate of the ship
	int x;
	int y;
	String name;
	// the score
	int point;
	int offset = 0;
	ConsistentHash consistentHash;
	Hashtable userTable = null;

	// two vectors maintained by all the ship entities that helps to record how
	// many yellow/black barrels have been picked up
	private Vector<Integer> addition = new Vector<>();
	private Vector<Integer> deduction = new Vector<>();
	private int index = 999;

	// constructor
	public Ship(int x, int y, String name, ConsistentHash consistentHash, Hashtable userTable) {
		this.x = x;
		this.y = y;
		this.name = name;
		this.consistentHash = consistentHash;
		this.userTable = userTable;
		HashNode newNode = new HashNode(false, name);
		consistentHash.addHashNode(newNode);
		/*
		 * HashMap<String, Float> workerWithTaskPercentage =
		 * consistentHash.getWorkerWithTaskPercentage(); float rate =
		 * workerWithTaskPercentage.get(name); point = (int) (rate * 100);
		 */
	}

	public int updateIndex() {
		// Hashtable userTable = UmiServer.getUserTable();
		int i = 0;
		for (Enumeration users = userTable.keys(); users.hasMoreElements();) {
			String user = users.nextElement().toString();
			if (this.name.equals(user)) {
				this.index = i;
				return index;
			} else {
				i++;
			}
		}
		return -1;
	}

	public int getVectorSize() {
		return (addition.size() > deduction.size()) ? addition.size() : deduction.size();
	}

	public void addOneElementToVector() {
		addition.addElement(0);
		deduction.addElement(0);
	}

	public void reduceOneElementToVector(int indexToDelete) {
		addition.remove(indexToDelete);
		deduction.remove(indexToDelete);
	}

	public void printVectors() {
		System.out.println("--------" + name + "--------");
		System.out.println("addVec: ");
		for (Iterator<Integer> it = addition.iterator(); it.hasNext();) {
			int i = it.next();
			System.out.println(i);
		}
		System.out.println("deVec: ");
		for (Iterator<Integer> it = deduction.iterator(); it.hasNext();) {
			int i = it.next();
			System.out.println(i);
		}
	}

	// add 1 to its own position in the addition vector
	public void updateAdditionVector() {
		int i = addition.get(index);
		addition.set(index, i + 1);
	}

	// add 1 to its own position in the deduction vector
	public void updateDedutionVector() {
		int i = deduction.get(index);
		deduction.set(index, i + 1);
	}

	// calculation the sum of a vector
	public int getSumOfVector(String whichVector) {
		int sum = 0;
		if (whichVector.equals("addition")) {
			for (Iterator<Integer> it = addition.iterator(); it.hasNext();) {
				sum += it.next();
			}
			return sum;
		} else if (whichVector.equals("deduction")) {
			for (Iterator<Integer> it = deduction.iterator(); it.hasNext();) {
				sum += it.next();
			}
			return sum;
		} else
			return -1;
	}

	public int getValueInVectorByIndex(String whichVector, int index) {
		if (whichVector.equals("addition")) {
			return addition.get(index);
		} else if (whichVector.equals("deduction")) {
			return deduction.get(index);
		} else {
			return -1;
		}
	}

	public void setVectorValueByIndex(String whichVector, int newValue, int index) {
		if (whichVector.equals("addition")) {
			addition.set(index, newValue);
		} else if (whichVector.equals("deduction")) {
			deduction.set(index, newValue);
		}
	}

	public void refreshPoint() {
		try {
			HashMap<String, Float> workerWithTaskPercentage = consistentHash.getWorkerWithTaskPercentage();
			float rate = workerWithTaskPercentage.get(name);
			int numberOfTask = consistentHash.getNumberOfTask();
			this.point = (int) (rate * numberOfTask);
		} catch (Exception e) {
		}
	}

	public void refreshOffset(int x) {
		this.offset = offset + x;
	}

	// left method
	public void left() {
		x -= 10;
		// link left edge and the right one together
		if (x < 0)
			x += 256;
	}

	// right method
	public void right() {
		x += 10;
		// link right edge and the left one together
		x %= 256;
	}

	// up method
	public void up() {
		y += 10;
		// link upper edge and the lower one together
		y %= 256;
	}

	// down method
	public void down() {
		y -= 10;
		// link lower edge and the upper one together
		if (y < 0)
			y += 256;
	}
}