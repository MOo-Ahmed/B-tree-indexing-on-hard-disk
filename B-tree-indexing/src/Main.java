import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import java.io.File;  

public class Main {
	static int m ;

	public static void main(String[] args) throws IOException {
		Main m = new Main();
		m.start();
	}
	
	void test1(int m, String filename) throws IOException {
		CreateIndexFileFile(filename, 5, 4);
		DisplayIndexFileContent(filename);
	}
	void test2(int m, String filename) throws IOException{
		//Insert: 5, 3, 21, 9, 1, 13, 2,7,10 and then show the index file content.
		this.m = m ;
		System.out.println(InsertNewRecordAtIndex(filename, 5 , 100));
		System.out.println(InsertNewRecordAtIndex(filename, 3 , 101));
		System.out.println(InsertNewRecordAtIndex(filename, 21 , 102));
		System.out.println(InsertNewRecordAtIndex(filename, 9 , 103));
		System.out.println(InsertNewRecordAtIndex(filename, 1 , 104));
		System.out.println(InsertNewRecordAtIndex(filename, 13 , 105));
		System.out.println(InsertNewRecordAtIndex(filename, 2 , 106));
		System.out.println(InsertNewRecordAtIndex(filename, 7 , 107));
		System.out.println(InsertNewRecordAtIndex(filename, 10 , 108));
		DisplayIndexFileContent(filename);
	}
	
	void test3and4(int m, String filename) throws IOException {
		this.m = m ;
		//Delete 10 and then show the index file content
		if(DeleteRecordFromIndex(filename, 10) == false) {
			System.out.println("The record with id = 10 , doesn't exist in index file ");
		}
		else	System.out.println("Successfully deleted 10");
		DisplayIndexFileContent(filename);
	}
	
	void test5(int m, String filename) throws IOException {
		this.m = m ;
		//Delete 21 and then show the index file content
		if(DeleteRecordFromIndex(filename, 21) == false) {
			System.out.println("The record with id = 21 , doesn't exist in index file ");
		}
		else	System.out.println("Successfully deleted 21");
		DisplayIndexFileContent(filename);
	}
	

	private void start() throws IOException {
		String filename = "B-TreeIndex.bin" ;
		//test1(4, filename);
		//test2(4, filename);
		//test3and4(4, filename);
		//test5(4, filename);
	}


	// First required function, Finished
	void CreateIndexFileFile(String filename, int numberOfRecords, int m) throws IOException {
		this.m = m;
		File f= new File(filename);        
		f.delete()  ;       
		RandomAccessFile file = new RandomAccessFile(filename, "rw");
		file.seek(0);
		for (int i = 1; i <= numberOfRecords; i++) {
			file.writeInt(-1); // write the leaf indicator
			if (i != numberOfRecords)
				file.writeInt(i);
			else
				file.writeInt(-1);
			file.writeInt(-1);
			for (int j = 0; j < (m - 1); j++) {
				file.writeInt(-1);
				file.writeInt(-1);
			}
		}
		file.close();
	}

	boolean isFileComplete(String filename) throws IOException {
		boolean isFull = true ; ;
		RandomAccessFile file = new RandomAccessFile(filename, "rw");
		file.seek(4*(2 * m + 1));
		for(int i = 1 ; i < m ; i++) {
			Page temp = readPage(file);
			if ((!temp.isComplete() && temp.leafIndicator == 0)	|| temp.leafIndicator == -1 ){
				isFull = false ;
				break ;
			}
		}
		file.close();
		return isFull ;
	}
	
	// Second required function , Finished except the case when you insert but the file is complete 
	int InsertNewRecordAtIndex(String filename, int RecordID, int Reference) throws IOException {
		// insert function should return -1 if there is no place to insert the record
		// or the index of the node where the new record is inserted if the record was
		// inserted successfully.
		int returnIndex = -1;
		if(isFileComplete(filename) == true) {
			return -1 ;
		}
		RandomAccessFile file = new RandomAccessFile(filename, "rw");
		file.seek(4);
		Node node = new Node(RecordID, Reference);
		int nextEmptyPlace = file.readInt();
		if (nextEmptyPlace == 1) {
			// Our tree is empty , this is the first node in the first page
			file.seek(4 * (2 * m + 1));
			Page oldPage = readPage(file);
			insertAtNonCompletePage(file, oldPage, node, 1);
			file.close();
			returnIndex = 1;
		} else if (nextEmptyPlace == 2) {
			// Our tree has just 1 page , this page is either fully occupied or not
			file.seek(4 * (2 * m + 1));
			Page oldPage = readPage(file);
			if (oldPage.indexOfEmptyNode() == -1) {
				// Fully occupied , we need to split it
				returnIndex = insertAndMakePageItselfParent(file, oldPage, node , 1);
			} else {
				// Still has some nodes to insert at , no need to split the page
				insertAtNonCompletePage(file, oldPage, node, 1);
				returnIndex = 1;
			}
		} else {
			// Our tree has more than one page , we should select the appropriate one
			returnIndex = complexInsertionHandler(file, node);
		}
		file.close();
		return returnIndex;
	}


	int insertAtNonCompletePage(RandomAccessFile file, Page page, Node node, int pageIndex) throws IOException {
		if (page.leafIndicator == -1) {
			// This means the page is empty, we insert its first node , we should update its leaf indicator
			int nextEmptyPlace = -999;
			page.leafIndicator = 0;
			nextEmptyPlace = page.records.get(0).Key;
			page.records.get(0).Equals(new Node(-1, -1));

			file.seek(4);
			if (file.readInt() == pageIndex) {
				//This means the current page was the first empty page, we should update that to the page after it.
				file.seek(4);
				file.writeInt(nextEmptyPlace);
			}
		}
		int correctIndex = page.getCorrectIndexOfNewNode(node);
		int oldLargestKey = page.getLargestKey();
		if(correctIndex == -2) {
			page.records.add(node);
		}
		else	page.records.add(correctIndex, node);
		file.seek(4 * pageIndex * (2 * m + 1));
		WritePage(file, page);
		if(oldLargestKey < node.Key) {
			replaceOldKey(oldLargestKey, node.Key, file);
		}
		return pageIndex ;
	}


	int insertAndMakePageItselfParent(RandomAccessFile file, Page oldPage, Node node , int pageIndex) throws IOException {
		// Choose this function in case you want to insert and split the page , but the page itself will be a parent
		// If you want to insert a node in a complete page by making a new parent above and a new page down , this function doesn't fit
		// Example that doesn't fit is Lab 5 , slide 6 , when we try to insert 2
		//________________________________________________________________________________________________________________________
		int correctIndex = oldPage.getCorrectIndexOfNewNode(node);
		ArrayList<Node> records = new ArrayList<Node>();
		records.addAll(oldPage.records);
		if(correctIndex == -2) {
			records.add(node);
		}
		else	records.add(correctIndex, node);
		
		int currentPageIndex = pageIndex, nextEmptyPlace, secondNextEmptyPlace;
		file.seek(4);
		nextEmptyPlace = file.readInt();
		file.seek(nextEmptyPlace * (4 * (2 * m + 1)) + 4);
		secondNextEmptyPlace = file.readInt();

		int middleIndex = records.size() / 2 - 1;
		Node middleNode = new Node(), lastNode = new Node();
		middleNode.Equals(records.get(middleIndex));
		lastNode.Equals(records.get(records.size() - 1));
		middleNode.Reference = nextEmptyPlace;
		lastNode.Reference = secondNextEmptyPlace;

		oldPage.leafIndicator = 1;
		oldPage.records.clear();
		oldPage.records.add(middleNode);
		oldPage.records.add(lastNode);
		while (oldPage.records.size() < m) {
			oldPage.records.add(new Node(-1, -1));
		}
		file.seek(currentPageIndex * 4 * (2 * m + 1));
		WritePage(file, oldPage);

		ArrayList<Node> leftRecords = new ArrayList<Node>();
		ArrayList<Node> rightRecords = new ArrayList<Node>();
		for (int i = 0; i < records.size(); i++) {
			if (i <= middleIndex) {
				leftRecords.add(records.get(i));
			} else {
				rightRecords.add(records.get(i));
			}
		}
		Page leftPage = new Page(m, 0, leftRecords);
		Page rightPage = new Page(m, 0, rightRecords);

		while (leftPage.records.size() < m) {
			leftPage.records.add(new Node(-1, -1));
		}
		while (rightPage.records.size() < m) {
			rightPage.records.add(new Node(-1, -1));
		}

		file.seek(nextEmptyPlace * (4 * (2 * m + 1)));
		WritePage(file, leftPage);
		file.seek(secondNextEmptyPlace * (4 * (2 * m + 1)) + 4);
		int newEmptyPlace = file.readInt();
		file.seek(4);
		file.writeInt(newEmptyPlace);
		file.seek(secondNextEmptyPlace * 4 * (2 * m + 1));
		WritePage(file, rightPage);
		if (node.Key > middleNode.Key) {
			return secondNextEmptyPlace;
		} else {
			return nextEmptyPlace;
		}
	}


	int complexInsertionHandler(RandomAccessFile file, Node node) throws IOException {
		int pageIndex = getPageIndex(node.Key, file);
		int index = -1 ;
		file.seek(pageIndex*4*(2*m + 1));
		Page page = readPage(file);		
		int parentPageIndex = getParentNodeIndex(page.getLargestKey(), pageIndex, file);
		file.seek(parentPageIndex*4*(2*m+1));
		Page parentPage = readPage(file);
		if(page.isComplete() ) {
			// This means it's a full page
			if(parentPage.isComplete()) {
				// even the parent page is full , we need to add new level of parents
				return  insertAndMakeNewLevelOfParents(file, node, page, pageIndex, parentPageIndex);
			}
			// we need to split only the current page , and insert a new largest key at parent node
			else	return insertAndSplitCurrentAndMakeNewParent(file, node, page, pageIndex);
		}
		else {
			// simple case , just insert the new node at its page , no extra procedures needed 
			index = insertAtNonCompletePage(file, page, node, pageIndex);
		}
		return index;
	}


	int insertAndMakeNewLevelOfParents(RandomAccessFile file, Node node, Page page, int pageIndex, int parentPageIndex) throws IOException {
		// This function is specified for the case you wanna insert a node , but both the page and parent page are full
		//You gotta make a new root page
		int correctIndex = page.getCorrectIndexOfNewNode(node);
		int oldLargestKey = page.getLargestKey();

		ArrayList<Node> records = new ArrayList<Node>();
		records.addAll(page.records);
		if(correctIndex == -2) {
			records.add(node);
		}
		else	records.add(correctIndex, node);
		
		file.seek(4);
		int firstEmptyPlace = file.readInt();
		file.seek(firstEmptyPlace*4*(2*m+1)+4);
		int secondEmptyPlace = file.readInt();
		file.seek(secondEmptyPlace*4*(2*m+1)+4);
		int thirdEmptyPlace = file.readInt();
		file.seek(thirdEmptyPlace*4*(2*m+1)+4);
		int nextEmptyPlace = file.readInt() ;
		
		sortList(records);
		int middleIndex = records.size() / 2 - 1;
		
		Node middleNode = new Node(), lastNode = new Node();
		middleNode.Equals(records.get(middleIndex));
		lastNode.Equals(records.get(records.size() - 1));

		ArrayList<Node> rightRecords = new ArrayList<Node>();
		ArrayList<Node> leftRecords = new ArrayList<Node>();
		for(int i = 0 ; i < records.size() ; i++) {
			Node temp = records.get(i);
			if(i <= middleIndex) {
				leftRecords.add(temp);
			}
			else	rightRecords.add(temp);
		}
		while(leftRecords.size() < m)	leftRecords.add(new Node(-1,-1));
		while(rightRecords.size() < m)	rightRecords.add(new Node(-1,-1));


		page.records.clear();
		page.records.addAll(leftRecords);
		page.sort();
		file.seek(pageIndex*4*(2*m+1));
		WritePage(file, page);


		Page newPage = new Page(m, 0, rightRecords);
		newPage.sort();
		file.seek(firstEmptyPlace*4*(2*m+1));
		WritePage(file, newPage);

		file.seek(4);
		file.writeInt(nextEmptyPlace);

		file.seek(parentPageIndex*4*(2*m+1));
		Page ParentPage = readPage(file);
		ParentPage.updateKey(oldLargestKey, middleNode.Key);
		
		middleNode.Reference = pageIndex ;
		lastNode.Reference = firstEmptyPlace ;
		ParentPage.records.add(lastNode);
		
	
		records.clear();
		records.addAll(ParentPage.records);
		Node newMiddle = new Node() , newLast = new Node();
		sortList(records);
		
		middleIndex = records.size() / 2 - 1;
		newMiddle.Equals(records.get(middleIndex));
		newLast.Equals(records.get(records.size() - 1));

		rightRecords.clear();
		leftRecords.clear();
		for(int i = 0 ; i < records.size() ; i++) {
			Node temp = records.get(i);
			if(i <= middleIndex) {
				leftRecords.add(temp);
			}
			else	rightRecords.add(temp);
		}
		while(leftRecords.size() < m)	leftRecords.add(new Node(-1,-1));
		while(rightRecords.size() < m)	rightRecords.add(new Node(-1,-1));

		Page newLeft = new Page(m,1,leftRecords);
		Page newRight = new Page(m,1,rightRecords);
		newMiddle.Reference = secondEmptyPlace ;
		newLast.Reference = thirdEmptyPlace ;
		newLeft.sort();
		newRight.sort();
		file.seek(secondEmptyPlace*4*(2*m+1));
		WritePage(file, newLeft);
		file.seek(thirdEmptyPlace*4*(2*m+1));
		WritePage(file, newRight);
		
		ParentPage.records.clear();
		ParentPage.records.add(newMiddle);
		ParentPage.records.add(newLast);
		while(ParentPage.records.size() < m) {
			ParentPage.records.add(new Node(-1,-1));
		}
		ParentPage.sort();
		file.seek(parentPageIndex*4*(2*m+1));
		WritePage(file, ParentPage);
		
		
		file.seek(4*(2*m+1));
		return getPageIndex(node.Key, file);
	}


	int insertAndSplitCurrentAndMakeNewParent(RandomAccessFile file, Node node , Page oldPage , int pageIndex) throws IOException  {
			// Here we've to make a new page, and assign half of the keys to it, the first half remains in oldPage
			int correctIndex = oldPage.getCorrectIndexOfNewNode(node);
			int oldLargestKey = oldPage.getLargestKey();

			ArrayList<Node> records = new ArrayList<Node>();
			records.addAll(oldPage.records);
			if(correctIndex == -2) { 
				// This means the key is larger than all the keys , so it should be inserted at the end of the page 
				records.add(node);
			}
			else	records.add(correctIndex, node);
			
			file.seek(4);
			int nextEmptyPlace = file.readInt();
			file.seek(nextEmptyPlace*4*(2*m+1)+4);
			int newEmptyPlace = file.readInt();
			int middleIndex = records.size() / 2 - 1;
			Node middleNode = new Node(), lastNode = new Node();
			middleNode.Equals(records.get(middleIndex));
			lastNode.Equals(records.get(records.size() - 1));

			ArrayList<Node> rightRecords = new ArrayList<Node>();
			ArrayList<Node> leftRecords = new ArrayList<Node>();
			for(int i = 0 ; i < records.size() ; i++) {
				Node temp = records.get(i);
				if(i <= middleIndex) {
					leftRecords.add(temp);
				}
				else	rightRecords.add(temp);
			}
			while(leftRecords.size() < m)	leftRecords.add(new Node(-1,-1));
			while(rightRecords.size() < m)	rightRecords.add(new Node(-1,-1));


			oldPage.records.clear();
			oldPage.records.addAll(leftRecords);

			file.seek(pageIndex*4*(2*m+1));
			WritePage(file, oldPage);


			Page newPage = new Page(m, 0, rightRecords);
			file.seek(nextEmptyPlace*4*(2*m+1));
			WritePage(file, newPage);

			file.seek(4);
			file.writeInt(newEmptyPlace);

			int parentIndex = getParentNodeIndex(oldLargestKey, pageIndex, file);
			file.seek(parentIndex*4*(2*m+1));
			Page ParentPage = readPage(file);
			ParentPage.updateKey(oldLargestKey, middleNode.Key);
			lastNode.Reference = nextEmptyPlace ;
			insertAtNonCompletePage(file, ParentPage, lastNode, parentIndex);
			file.seek(4*(2*m+1));
			return getPageIndex(node.Key, file);
	}


	Page readPage(RandomAccessFile file) throws IOException {
		int leafOrNotLeaf = file.readInt();
		ArrayList<Node> nodes = new ArrayList<Node>();
		for (int i = 0; i < m; i++) {
			Node temp = new Node(file.readInt(), file.readInt());
			nodes.add(temp);
		}
		Page page = new Page(m, leafOrNotLeaf, nodes);
		return page;
	}


	void WritePage(RandomAccessFile file, Page page) throws IOException {
		file.writeInt(page.leafIndicator);
		for (int i = 0; i < m; i++) {
			file.writeInt(page.records.get(i).Key);
			file.writeInt(page.records.get(i).Reference);
		}
	}


	// Fourth required function , Finished
	void DisplayIndexFileContent(String filename) throws FileNotFoundException, IOException {
		// this method should display content of the file, each node in a line.
		DisplayFile(new RandomAccessFile(filename, "rw"), m);
	}


	void DisplayFile(RandomAccessFile file, int m) throws IOException {
		this.m = m;
		file.seek(0);
		int numOfRecords = (int) (file.length() / (4 * (2 * m + 1)));
		System.out.println("____________________________________________________________________________________________");
		for (int i = 0; i < numOfRecords; i++) {
			System.out.print((i) + ".\t" + file.readInt() + "\t");
			for (int j = 0; j < m; j++) {
				System.out.print("(" + file.readInt() + "\t" + file.readInt() + ")\t");
			}
			System.out.println();
		}
		System.out.println("____________________________________________________________________________________________");

		file.close();
	}


	// Fifth required function , Finished
	int SearchARecord(String filename, int RecordID) throws IOException {
		// this method should return -1 if the record doesn’t exist
		// in the index or the reference value to the data file if the record exist on
		// the index
		// ___________________________________________________________________________________________
		RandomAccessFile file = new RandomAccessFile(filename, "rw");
		file.seek(4 * (2 * m + 1));
		int result = search(RecordID, file);
		file.close();
		return result;
	}


	int search(int RecordID, RandomAccessFile file) throws IOException {
		// this method should return -1 if the record doesn’t exist
		// in the index or the reference value to the data file if the record exist on
		// the index
		// ___________________________________________________________________________________________

		Page page = readPage(file);
		for (int i = 0; i < m; i++) {
			if (page.records.get(i).Key == RecordID) {
				if (page.leafIndicator == 0) {
					return page.records.get(i).Reference;
				} else {
					file.seek(page.records.get(i).Reference * 4 * (2 * m + 1));
					return search(RecordID, file);
				}
			} else if (page.records.get(i).Key > RecordID) {
				if(page.leafIndicator == 1) {
					file.seek(page.records.get(i).Reference * 4 * (2 * m + 1));
					return search(RecordID, file);
				}
				else {
					break;
				}
			}

		}
		return -1;
	}


	int getPageIndex (int key, RandomAccessFile file) throws IOException {
		file.seek(4*(2*m + 1));
		Page page = readPage(file);
		int idx = 0 , largestKey = page.getLargestKey();
		if( largestKey >= key ) {

			file.seek(4*(2*m + 1));
			search(key, file); // we call it to know which page it will stop at
			idx = (int) (file.getFilePointer()/(4*(2*m + 1))) - 1 ;
		}
		else {
			return getPageIndex(largestKey, file);
		}
		return idx;
	}


	void replaceOldKey(int oldKey , int newKey , RandomAccessFile file) throws IOException {
		file.seek(4*(2*m + 1));
		for(int i = 0 ; i < m ; i++) {
			Page page = readPage(file);
			if(page.doIHaveThisNode(newKey) != null) {
				break ;
			}
			page.updateKey(oldKey, newKey);
			int seek = (int) (file.getFilePointer()- 4*(2*m + 1));
			file.seek(seek);
			WritePage(file, page);
		}
	}


	int getParentNodeIndex(int key , int ref , RandomAccessFile file) throws IOException {
		file.seek(4*(m*2 + 1));
		boolean isFound = false ;
		while(!isFound) {
			Page tempPage = readPage(file);
			if(tempPage.leafIndicator < 0) {
				break ;
			}
			else if(tempPage.leafIndicator == 1){
				Node tempNode = tempPage.doIHaveThisNode(key); 
				if(tempNode != null) {
					if(tempNode.Reference == ref) {
						isFound = true ;
						int idx = (int) ((file.getFilePointer()) / (4*(2*m + 1))) -1 ;
						return idx ;
					}
					else {
						file.seek(tempNode.Reference*4*(2*m+1));
						continue;
					}
				}
				
			}
		}
		return -1;
	}


	// Third required function
	boolean DeleteRecordFromIndex(String filename, int RecordID) throws IOException {
		boolean doesNodeExist = true ;
		if(SearchARecord(filename, RecordID) == -1) {
			// Then the record doesn't exist
			// You can't delete it
			return false ;
		}
		RandomAccessFile file = new RandomAccessFile(filename, "rw");
		int pageIndex = getPageIndex(RecordID, file);
		file.seek(pageIndex*4*(2*m+1));
		Page page = readPage(file);
		if(page.isAboveMinimumDescendants() == true) {
			deleteFromPageAboveMinimum(file, page, RecordID, pageIndex);
		}
		else if(page.isEqualToMinimumDescendants() == true) {
			deleteFromPageEqualToMinimum(file, page, RecordID, pageIndex);
		}
		
		file.close();
		return doesNodeExist ;
	}


	void deleteFromPageAboveMinimum(RandomAccessFile file, Page oldPage, int Key , int pageIndex) throws IOException {
		int indexOfNodeToRemove = oldPage.indexOfNode(Key);
		if(indexOfNodeToRemove == -1)	return ;
		int oldLargestKey = oldPage.getLargestKey();
		oldPage.records.remove(indexOfNodeToRemove);
		if(oldLargestKey == Key) {
			int newKey = oldPage.getLargestKey();
			replaceOldKey(oldLargestKey, newKey, file);
		}
		while(oldPage.records.size() < m) {
			oldPage.records.add(new Node(-1,-1));
		}
		file.seek(pageIndex*4*(2*m+1));
		WritePage(file, oldPage);
	}


	void deleteFromPageEqualToMinimum(RandomAccessFile file, Page oldPage, int Key , int pageIndex) throws IOException  {
		int LargestKeyAtOldPage = oldPage.getLargestKey();
		int parentPageIndex = getParentNodeIndex(LargestKeyAtOldPage, pageIndex, file);
		file.seek(parentPageIndex*4*(2*m+1));
		Page parentPage = readPage(file);
		int oldPageIndexInParentPage = parentPage.indexOfNode(LargestKeyAtOldPage);
		Page rightSiblingPage = null , leftSiblingPage = null;
		if(oldPageIndexInParentPage == 0) {
			//The page is leftmost , if you intend to merge or borrow from a sibling page , the sibling should be at right only
			int siblingPageIndex = parentPage.records.get(1).Reference ;
			file.seek(siblingPageIndex*4*(2*m+1));
			rightSiblingPage = readPage(file);
			if(rightSiblingPage.isAboveMinimumDescendants()) {
				// Take a node from the sibling, insert it into the old page
				Node minimumNode = new Node();
				minimumNode.Equals(rightSiblingPage.records.get(0));
				deleteFromPageAboveMinimum(file,rightSiblingPage, minimumNode.Key, siblingPageIndex);
				insertAtNonCompletePage(file, oldPage, minimumNode, pageIndex);
				deleteFromPageAboveMinimum(file, oldPage, Key, pageIndex);
				replaceOldKey(Key, oldPage.getLargestKey(), file);
			}
			else  {
				// There should be merge, delete one of them
				ArrayList<Node> siblingRecords = new ArrayList<Node> ();
				siblingRecords.addAll(rightSiblingPage.records);
				deletePage(file, siblingPageIndex);
				oldPage.records.addAll(siblingRecords);
				sortList(oldPage.records);
				deleteAllPageReferences(file, rightSiblingPage.getLargestKey());
				deleteFromPageAboveMinimum(file, oldPage, Key, pageIndex);
				file.seek(pageIndex*4*(2*m+1));
				WritePage(file, oldPage);
				replaceOldKey(Key, oldPage.getLargestKey(), file);
				file.seek(4);
				int firstEmptyPlace = file.readInt();
				if(firstEmptyPlace > siblingPageIndex) {
					file.seek(4);
					file.writeInt(siblingPageIndex);
					file.seek(siblingPageIndex*4*(2*m+1)+4);
					file.writeInt(firstEmptyPlace);
				}
				else if(firstEmptyPlace < siblingPageIndex && firstEmptyPlace != -1) {
					file.seek(firstEmptyPlace*4*(2*m+1)+4);
					int nextEmptyPlace = file.readInt();
					file.seek(firstEmptyPlace*4*(2*m+1)+4);
					file.writeInt(siblingPageIndex);
					file.seek(siblingPageIndex*4*(2*m+1)+4);
					file.writeInt(nextEmptyPlace);
				}
				else {
					file.seek(4);
					file.writeInt(siblingPageIndex);
				}
				
			}

		}
		else if(oldPageIndexInParentPage <= m-1) {
			// It has a L.H.S sibling
			int leftSiblingIndex = parentPage.records.get(oldPageIndexInParentPage-1).Reference;
			file.seek(leftSiblingIndex*4*(2*m+1));
			leftSiblingPage = readPage(file);
			//file.seek(rightSiblingIndex*4*(2*m+1));
			//rightSiblingPage = readPage(file);

			if(leftSiblingPage.isAboveMinimumDescendants()) {
				int siblingBorrowedNodeIndex = leftSiblingPage.indexOfNode(leftSiblingPage.getLargestKey());
				Node temp =  leftSiblingPage.records.get(siblingBorrowedNodeIndex);
				oldPage.records.add(temp);
				deleteFromPageAboveMinimum(file, leftSiblingPage,leftSiblingPage.getLargestKey() , leftSiblingIndex);
				deleteFromPageAboveMinimum(file, oldPage, Key, pageIndex);
				insertAtNonCompletePage(file, oldPage, temp, pageIndex);

			}
			else if(leftSiblingPage.isEqualToMinimumDescendants()){
				for(int i = 0 ; i < leftSiblingPage.ActualSize();i++) {
					insertAtNonCompletePage(file, oldPage, leftSiblingPage.records.get(i), pageIndex);
				}
				int leftSiblingLargestKey = leftSiblingPage.getLargestKey();
				deletePage(file, leftSiblingIndex);
				deleteAllPageReferences(file, leftSiblingLargestKey);
				deleteFromPageAboveMinimum(file, oldPage, Key, pageIndex);
				file.seek(4);
				int firstEmptyPlace = file.readInt();
				if(firstEmptyPlace > leftSiblingIndex) {
					file.seek(4);
					file.writeInt(leftSiblingIndex);
					file.seek(leftSiblingIndex*4*(2*m+1)+4);
					file.writeInt(firstEmptyPlace);
				}
				else if(firstEmptyPlace < leftSiblingIndex && firstEmptyPlace != -1) {
					file.seek(firstEmptyPlace*4*(2*m+1)+4);
					int nextEmptyPlace = file.readInt();
					file.seek(firstEmptyPlace*4*(2*m+1)+4);
					file.writeInt(leftSiblingIndex);
					file.seek(leftSiblingIndex*4*(2*m+1)+4);
					file.writeInt(nextEmptyPlace);
				}
				else {
					file.seek(4);
					file.writeInt(leftSiblingIndex);
				}
			}

			/*
			else if(rightSiblingPage.isAboveMinimumDescendants()) {
				Node temp =  rightSiblingPage.records.get(0);
				oldPage.records.add(temp);
				deleteFromPageAboveMinimum(file, rightSiblingPage,temp.Key , rightSiblingIndex);
				deleteFromPageAboveMinimum(file, oldPage, Key, pageIndex);
				insertAtNonCompletePage(file, oldPage, temp, pageIndex);

			}
			else if(rightSiblingPage.isEqualToMinimumDescendants()){
				for(int i = 0 ; i < rightSiblingPage.ActualSize();i++) {
					insertAtNonCompletePage(file, oldPage, rightSiblingPage.records.get(i), pageIndex);
				}
				int rightSiblingLargestKey = rightSiblingPage.getLargestKey();
				deletePage(file, leftSiblingIndex);
				deleteAllPageReferences(file, rightSiblingLargestKey);
				deleteFromPageAboveMinimum(file, oldPage, Key, pageIndex);
			}
			*/
		}
	}


	void deleteAllPageReferences(RandomAccessFile file, int Key) throws IOException {
		file.seek(4*(2*m+1));
		int index = 1, nextIndex = -1 ;
		Page tempPage = readPage(file);
		while(tempPage.doIHaveThisNode(Key) != null) {
			nextIndex = tempPage.doIHaveThisNode(Key).Reference;
			deleteFromPageAboveMinimum(file, tempPage, Key, index);
			file.seek(nextIndex*4*(2*m+1));
			tempPage = readPage(file);
			index = nextIndex ;
		}

	}


	void deletePage(RandomAccessFile file, int pageIndex) throws IOException {
		file.seek(pageIndex*4*(2*m+1));
		int leafIndicator = -1 ;
		ArrayList<Node> emptyRecords = new ArrayList<Node>();
		for(int i = 0; i < m; i++) {
			emptyRecords.add(new Node(-1,-1));
		}
		Page page = new Page(m, leafIndicator, emptyRecords);
		WritePage(file, page);

	}
	
	
	public void sortList(ArrayList<Node> records) {
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
			if(records.size() < m)	records.add(new Node(-1,-1));
		}
	}
}
