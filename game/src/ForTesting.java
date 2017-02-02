// this file is only for testing the algorithm

import java.awt.image.RescaleOp;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.print.attribute.standard.PrinterLocation;
import javax.swing.text.html.HTMLDocument.Iterator;

import org.omg.CORBA.PRIVATE_MEMBER;

public class ForTesting {
	public static HashFunction hashFunction = new HashFunction();
	private static int NUMBEROFTASK = 100;
	
	public static void main(String[] args) {
		// it's only used for testing
		HashMap<String, Boolean> nodes = new HashMap<String, Boolean>();
		nodes.put("A", false);
		nodes.put("B", false);
		nodes.put("C", false);
		
		ConsistentHash consistentHash = new ConsistentHash(new HashFunction(), 1, NUMBEROFTASK);
	
		HashNode newNode_A = new HashNode(false, "A");
		consistentHash.addHashNode(newNode_A);
		
		HashNode newNode_B = new HashNode(false, "B");
		consistentHash.addHashNode(newNode_B);
		
		HashNode newNode_C = new HashNode(false, "C");
		consistentHash.addHashNode(newNode_C);
		
		printResult(consistentHash);
		consistentHash.removeHashNode("A");
		printResult(consistentHash);
		HashNode newNode = new HashNode(false, "D");
		consistentHash.addHashNode(newNode);
		printResult(consistentHash);
		
		consistentHash.reduceOneTask("C");
		printResult(consistentHash);
		consistentHash.addOneTask("C");
		consistentHash.addOneTask("C");
		printResult(consistentHash);
	}

	public static void printResult(ConsistentHash consistentHash) {
		System.out.println("---------------------------------------------");
		System.out.println("hash circle size: " + consistentHash.getTotalSize());
		System.out.println("workers size: " + consistentHash.getWorkersSize());
		System.out.println("The entire circle is: ");
		consistentHash.printNodes(consistentHash.getEntireCircle());
		consistentHash.printWorkers();
		try {
			for (int i = 0; i < consistentHash.getWorkersSize() + 1; i++) {
				System.out.printf("No.%d subMap: ", i + 1);
				System.out.print("\n");
				consistentHash.printNodes(consistentHash.getSubMaps().get(i));
			}
			HashMap<String, Float> workerWithTaskPercentage = consistentHash.getWorkerWithTaskPercentage();
			java.util.Iterator<String> it2 = workerWithTaskPercentage.keySet().iterator();
			while (it2.hasNext()) {
				String player = it2.next();
				Float rate = workerWithTaskPercentage.get(player);
				System.out.println("player: " + player + " Rate: " + rate);
			}
		} catch (Exception e) {
			System.out.println("something wrong with the printResult function");
		}
	}
}
