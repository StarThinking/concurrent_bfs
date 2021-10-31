/*
   A-B B-C
   GetMinDistance(A, B) = 1
   GetMinDistance(A, C) = 2
*/
import java.io.*;
import java.util.*;
import java.text.*;
import java.math.*;
import java.util.regex.*;

public class Solution {
    private Map<Integer, List<Integer>> adjList = new HashMap<Integer, List<Integer>>();
    private Deque<Integer> queue = new LinkedList<Integer>();
    private Set<Integer> visited = new HashSet<Integer>();
    private boolean foundFlag = false;
    private final int THREADS_NUM = 10;

    // build adjacency list
    private void buildGraph(List<int[]> edges) {
        for (int[] edge : edges) {
            adjList.putIfAbsent(edge[0], new ArrayList<Integer>());
            adjList.putIfAbsent(edge[1], new ArrayList<Integer>());
            adjList.get(edge[0]).add(edge[1]);
            adjList.get(edge[1]).add(edge[0]);
        }
    }

    public Solution(List<int[]> edges) {
	buildGraph(edges);
    }

    private synchronized void queueAdd(int id) {
	queue.add(id);
    }
    
    private synchronized boolean visitedContains(int id) {
	return visited.contains(id);
    }
    
    private synchronized void visitedAdd(int id) {
	visited.add(id);
    }

    private synchronized void updateFoundFlag() {
	foundFlag = true;
    }
   
    // the main function of a worker thread 
    private void func(Map<Thread, List<Integer>> thread2sublistMap, int node2) {
        for (int node : thread2sublistMap.get(Thread.currentThread())) {
            for (int neiNode : adjList.get(node)) {
                if (!visitedContains(neiNode)) {
                    visitedAdd(neiNode);
                    queueAdd(neiNode);
                    if (neiNode == node2) {
                        updateFoundFlag();
                    }
                }
            }
        }
    }

    public int getMinDistanceParallel(int node1, int node2) {
	// reset for each search
	queue.clear();
	visited.clear();
	foundFlag = false;

	// perform bfs
        int distance = 0;
        queue.add(node1);
        visited.add(node1);
        while (!queue.isEmpty()) {
            distance++;

	    // split all the nodes in the queue into (up to) THREADS_NUM partitions
	    List<Integer> nodesInQueue = new ArrayList<Integer>();
	    while (!queue.isEmpty()) 
		nodesInQueue.add(queue.poll());
	    List<Integer>[] subLists = partition(nodesInQueue, (int) (Math.ceil((double) nodesInQueue.size() / THREADS_NUM)));
    	    Map<Thread, List<Integer>> thread2sublistMap = new HashMap<Thread, List<Integer>>();

            // create #sublists threads, each thread will perform the work of func()
	    Runnable runnable = () -> {
		func(thread2sublistMap, node2);
            };
	    for (int i = 0; i < subLists.length; i++) {
	        Thread thread = new Thread(runnable);
		// assign a thread with its own node list to process
		thread2sublistMap.put(thread, subLists[i]);
		thread.start();
	    }
	    
	    try {
                // wait for all threads to finish
		for (Thread thread : thread2sublistMap.keySet())
	            thread.join();
                // check if any of worker thread has found node2
		// if so, return distance; otherwise, keep searching
		if (foundFlag == true)
		    return distance;
	    } catch(Exception e) {
		e.printStackTrace();
	    }
        }
        return -1;
    }

    public static void main(String[] args) {
	// create a graph with 5 x 5 x 5 edges. e.g., 0-1,0-2,0-3,0-4,0-5, 1-11,1-12,..., 11-111,11-112,...
        List<int[]> edges = new ArrayList<int[]>();
	for (int i = 1; i <= 5; i++) {
	    edges.add(new int[]{0, i});
	    for (int j = 1; j <= 5; j++) {
		edges.add(new int[]{i, i * 10 + j});
		for (int k = 1; k <= 5; k++) {	
		    edges.add(new int[]{i * 10 + j, (i * 10 + j) * 10 + k});
		}
	    }
	}
        System.out.println("edges.size = " + edges.size());	
	Solution solution = new Solution(edges);
        System.out.println("distance between node 0 and node 1: " + solution.getMinDistanceParallel(0, 1));
        System.out.println("distance between node 0 and node 11: " + solution.getMinDistanceParallel(0, 11));
        System.out.println("distance between node 0 and node 111: " + solution.getMinDistanceParallel(0, 111));
        System.out.println("distance between node 0 and node 6: " + solution.getMinDistanceParallel(0, 6));
    }
    
    // helper function
    public static<T> List[] partition(List<T> list, int n) {
        // get the size of the list
        int size = list.size();
 
        // Calculate the total number of partitions `m` of size `n` each
        int m = size / n;
        if (size % n != 0) {
            m++;
        }
 
        // create `m` empty lists
        List<T>[] partition = new ArrayList[m];
        for (int i = 0; i < m; i++) {
            partition[i] = new ArrayList();
        }
 
        // process each list element and add it to the corresponding
        // list based on its position in the original list
        for (int i = 0; i < size; i++) {
            int index = i / n;
            partition[index].add(list.get(i));
        }
 
        // return the lists
        return partition;
    }
}
