
// This class is the hashnode class, it's used to tell the difference between task nodes and player nodes.
import java.util.HashMap;

public class HashNode {
	public String value; // The name or value of the node
	public Boolean isTask; // used to judge whether its a task node(true) or a
							// player node(false)

	private static HashMap<String, Boolean> node = new HashMap<String, Boolean>();
	
	//constructor
	public HashNode(Boolean isTask, String value) {
		this.isTask = isTask;
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public Boolean getIsTask() {
		return isTask;
	}

	public HashMap<String, Boolean> getHashMap() {
		return node;
	}

	public boolean judgeIsTask() {
		if (isTask)
			return true;
		else
			return false;
	}
}
