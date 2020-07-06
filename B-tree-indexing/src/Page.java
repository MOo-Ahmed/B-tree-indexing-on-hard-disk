import java.util.ArrayList;

public class Page {
	/* 
	Each page here has a list of nodes. Each node has 2 integers , the first one represents the record ID ,
	 the second one either represents the physical offset(if the page is leaf) , 
	 or represents the index of page to which this node points. 
	*/
	int numberOfNodes ;
	int leafIndicator = 0 ; // if 0 -> leaf , if 1 -> non-leaf
	int currentCapacity = 0 ; // every time you add a node , increase this value
	ArrayList<Node> records = new ArrayList<Node>();
	
	public Page (int m) {
		numberOfNodes = m ;
	}
	
	public Page(int m , int leafIndicator , ArrayList<Node> nodes) {
		this.numberOfNodes = m ;
		this.leafIndicator = leafIndicator ;
		this.records = nodes ;
	}
	
	public int getLargestKey () {
		int max = -1 ;
		for(int i = 0 ; i < records.size() ; i++ ) {
			if(records.get(i).Key > max)	max = records.get(i).Key ;
		}
		return max;
	} 
	
	public int indexOfEmptyNode () {
		for(int i = 0 ; i < records.size() ; i++) {
			if (records.get(i).Key == -1 && records.get(i).Reference == -1) {
				return i ;
			}
		}
		return -1;
	}
	
	public boolean addNode(Node node) {
		int i = indexOfEmptyNode() ;
		if(i != -1) {
			records.set(i, node);
			return true;
		}
		else {
			return false ;
		}
	}
	
	public int indexOfNode (int key) {
		for(int i = 0 ; i < records.size() ; i++) {
			if(records.get(i).Key == key)	return i ;
		}
		return -1 ;
	}
	
	public int getCorrectIndexOfNewNode (Node node) {
		int i = indexOfEmptyNode() ;
		for(int j = 0 ; j < records.size(); j++) {
			if(records.get(j).compareTo(node) > 0) {
				i = j ;
				break;
			}
		}
		if(i == -1 && node.Key > getLargestKey()) {
			i = -2;
		}
		return i ;
	}
	
	public void deleteEmptyNodeAt (int index) {
		records.remove(index);
	}
	
	public boolean isComplete() {
		return indexOfEmptyNode() == -1 ;
	}
	
	public void updateKey(int oldKey , int newKey) {
		if(leafIndicator == 0 || leafIndicator == -1) {
			return ;
		}
		for(int i = 0 ; i < records.size() ; i++) {
			if(records.get(i).Key == oldKey) {
				records.get(i).Key = newKey ;
				break;
			}
		}
	}
	
	public Node doIHaveThisNode(int key) {
		Node temp = null;
		for(int i = 0 ; i < records.size() ; i++) {
			if(records.get(i).Key == key) {
				temp = new Node() ;
				temp.Equals(records.get(i));
				break;
			}
		}
		return temp;
	}
	
	public boolean isAboveMinimumDescendants() {
		return ActualSize() > Math.ceil(records.size()/2) ;
	}

	public boolean isEqualToMinimumDescendants() {
		return ActualSize() == Math.ceil(records.size()/2.0) ;
	}
	
	public int ActualSize() {
		int count = 0 ;
		for(int i = 0 ; i < records.size() ; i++) {
			if(records.get(i).Key == -1 && records.get(i).Reference == -1)		count++ ;
		}
		return records.size()-count ;
	}
	
	@Override
	public String toString () {
		return leafIndicator + " - " + records ;
	}
	
	public void sort() {
		Node temp = new Node();
		int n = records.size() ;
		for (int i = 1; i < n; i++) {
			for(int j = i ; j > 0 ; j--){
				if(records.get(j).Key < records.get(j-1).Key){
					temp.Equals(records.get(j));
					records.get(j).Equals( records.get(j-1));
					records.get(j-1).Equals(temp);
				}
			}
		}
		while(records.get(0).Key == -1) {
			records.remove(0);
			records.add(new Node(-1,-1));
		}
	}
}
