
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.IntToDoubleFunction;
import java.util.logging.Logger;

public class ConsistentHash {
	private final HashFunction hashFunction;
	private final int numberOfReplicas;
	// This treemap circle represents for the whole hash space circle, where all
	// the task nodes and the player nodes will be mapped on it via the static
	// hash function
	private SortedMap<Long, HashNode> circle = new TreeMap<Long, HashNode>();

	// These two arraylists help to store the name of the players and the
	// hashvalues of them
	private static ArrayList<String> workers = new ArrayList<String>();
	private static ArrayList<Long> workersHash = new ArrayList<Long>();

	// These two arraylists help to store the value of the tasks and the
	// hashvalues of them
	private ArrayList<Integer> task = new ArrayList<Integer>();
	private ArrayList<Long> taskHash = new ArrayList<Long>();

	// The elements in this arraylist are SortedMap, which contain two fields:
	// key: hashvalue. value: HashNode entity, its a self-defined class
	private ArrayList<SortedMap<Long, HashNode>> subMaps = new ArrayList<SortedMap<Long, HashNode>>();

	// This HashMap helps to store the tuples of player's name and the score
	// they will be allocated
	private HashMap<String, Float> workerWithTaskPercentage = new HashMap<String, Float>();
	private static int numberOfTask;

	// constructor
	public ConsistentHash(HashFunction hashFunction, int numberOfReplicas, int numberOfTask) {
		this.hashFunction = hashFunction;
		this.numberOfReplicas = numberOfReplicas;
		ConsistentHash.numberOfTask = numberOfTask;
		// put all the tasks in these two arraylist and also put them on the
		// hash space circle
		for (int i = 0; i < numberOfTask; i++) {
			HashNode node = new HashNode(true, String.valueOf(i));
			long hashValue = addHashNode(node);
			task.add(i);
			taskHash.add(hashValue);
		}
	}

	public int getNumberOfTask() {
		return numberOfTask;
	}

	public void refresh(String worker, Long hashValue) {
		Set<Long> sets = circle.keySet();
		SortedSet<Long> sortedSets = new TreeSet<Long>(sets);
		int index = 0;
		for (Long hashCode : sortedSets) {
			if (!circle.get(hashCode).getIsTask()) {
				if (worker.equals(circle.get(hashCode).getValue())) {
					workers.add(index, worker);
					workersHash.add(index, hashValue);
				}
				index++;
			}
		}
	}

	// put one node on the hash space circle
	public Long addHashNode(HashNode node) {
		String worker = node.getValue();
		long hashValue = hashFunction.hash(node.getValue());
		circle.put(hashValue, node);
		if (!node.getIsTask()) {
			refresh(worker, hashValue);
		}
		return hashValue;
	}

	// remove one node from the hash space circle
	public void removeHashNode(String worker) {
		try {
			int indexOfWorker = workers.indexOf(worker);
			long hashOfWorker = workersHash.get(indexOfWorker);
			circle.remove(hashOfWorker);
			workers.remove(indexOfWorker);
			workersHash.remove(indexOfWorker);
		} catch (Exception e) {
			System.out.printf("worker %s not found! plz check", worker);
		}
	}

	// use this function for adding a task node on the hash space circle
	// in the game, if a player has hit one yellow barrel, then we use this
	// function to add a task node whose location is in front of this player
	// node
	public void addOneTask(String worker) {
		int indexOfWorker = workers.indexOf(worker);
		if (indexOfWorker == 0) {
			indexOfWorker = workers.size() - 1;
		} else {
			indexOfWorker = indexOfWorker - 1;
		}
		long hashOfWorker = workersHash.get(indexOfWorker);
		Set<Long> sets = circle.keySet();
		SortedSet<Long> sortedSets = new TreeSet<Long>(sets);
		for (Iterator iter = sortedSets.iterator(); iter.hasNext();) {
			if ((Long) iter.next() == hashOfWorker) {
				Long insertValue = (Long) iter.next() - 1;
				HashNode insertHashNode = new HashNode(true, "newNode");
				circle.put(insertValue, insertHashNode);
				// subMaps.clear();
				// workerWithTaskPercentage.clear();
				numberOfTask++;
				break;
			}
		}
	}

	// if a player has hit a black node, we use this function to reduce one task
	// node whose location is in front of him
	public void reduceOneTask(String worker) {
		int indexOfWorker = workers.indexOf(worker);
		if (indexOfWorker == 0) {
			indexOfWorker = workers.size() - 1;
		} else {
			indexOfWorker = indexOfWorker - 1;
		}
		long hashOfWorker = workersHash.get(indexOfWorker);
		// System.out.println(workers.size());
		Set<Long> sets = circle.keySet();
		SortedSet<Long> sortedSets = new TreeSet<Long>(sets);
		for (Iterator iter = sortedSets.iterator(); iter.hasNext();) {
			if ((Long) iter.next() == hashOfWorker) {
				Long hashOfNext = (Long) iter.next();
				circle.remove(hashOfNext);
				// subMaps.clear();
				// workerWithTaskPercentage.clear();
				numberOfTask--;
				break;
			}
		}
	}

	// return the subMaps separated by player nodes on the circle
	public ArrayList<SortedMap<Long, HashNode>> getSubMaps() {
		try {
			subMaps.clear();
			subMaps.add(circle.headMap(workersHash.get(0)));
			for (int i = 0; i < workersHash.size() - 1; i++) {
				long hash1 = workersHash.get(i);
				long hash2 = workersHash.get(i + 1);
				SortedMap<Long, HashNode> subMap = circle.subMap(hash1, hash2);
				subMaps.add(subMap);
			}
			subMaps.add(circle.tailMap(workersHash.get(workersHash.size() - 1)));
			return subMaps;
		} catch (Exception e) {
			System.out.printf("something wrong with the submaps");
			return null;
		}

	}

	public long getTotalSize() {
		return circle.size();
	}

	public long getWorkersSize() {
		return workers.size();
	}

	public SortedMap<Long, HashNode> getEntireCircle() {
		return circle;
	}

	// use this function to calculate the percentage of tasks allocated for each
	// worker.
	public HashMap<String, Float> getWorkerWithTaskPercentage() {
		SortedMap<Long, HashNode> headSubMap = circle.headMap(workersHash.get(0));
		SortedMap<Long, HashNode> tailSubMap = circle.tailMap(workersHash.get(workersHash.size() - 1));
		int numberOfSubTask = headSubMap.size() + tailSubMap.size() - 1;
		workerWithTaskPercentage.clear();
		workerWithTaskPercentage.put(workers.get(0), ((float) numberOfSubTask / numberOfTask)); // link the rear and the head together
		// System.out.println("player: " + workers.get(0) + " SpeedRate: " +
		// ((float) numberOfSubTask / numberOfTask));

		for (int i = 0; i < workersHash.size() - 1; i++) {
			long hash1 = workersHash.get(i);
			long hash2 = workersHash.get(i + 1);
			SortedMap<Long, HashNode> subMap = circle.subMap(hash1, hash2);
			workerWithTaskPercentage.put(workers.get(i + 1), (((float) subMap.size() - 1) / numberOfTask));
		}
		return workerWithTaskPercentage;
	}

	public void printWorkers() {
		Set<Long> sets = circle.keySet();
		SortedSet<Long> sortedSets = new TreeSet<Long>(sets);
		for (Long hashCode : sortedSets) {
			if (!circle.get(hashCode).getIsTask()) {
				System.out.println(circle.get(hashCode).getValue() + " " + hashCode);
			}
		}
	}

	public void printNodes(SortedMap<Long, HashNode> sortedmap) {
		Set<Long> sets = sortedmap.keySet();
		SortedSet<Long> sortedSets = new TreeSet<Long>(sets);
		for (Long hashCode : sortedSets) {
			System.out.print("hash: " + hashCode);
			HashNode node = circle.get(hashCode);
			String value = node.getValue();
			Boolean isTask = node.getIsTask();
			System.out.println("  value: " + value + " isTask: " + isTask);
		}
	}
}

class MyComparator implements Comparator<HashNode> {
	@Override
	public int compare(HashNode n1, HashNode n2) {
		String str1 = n1.getValue();
		String str2 = n2.getValue();
		return str1.compareTo(str2);
	}
}
