import java.io.IOException;
import java.io.RandomAccessFile;

public class Node implements Comparable<Node>{
	public int Key , Reference ;
	
	public Node(int key, int offset) {
		Key = key ;
		Reference = offset ;
	}
	
	public Node() {
		// TODO Auto-generated constructor stub
	}
	
	public void Equals (Node o) {
		this.Key = o.Key ;
		this.Reference = o.Reference ;
	}

	void WriteToFile(RandomAccessFile file) throws IOException {
		file.writeInt(Key);
		file.writeInt(Reference);
	}
	
	@Override
	public String toString () {
		return Key + " - " + Reference ;
	}

	@Override
	public int compareTo(Node o) {
		return Key - o.Key;
	}

	

}
