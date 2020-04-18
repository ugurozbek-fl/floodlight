package net.floodlightcontroller.topology.tunahan;

import net.floodlightcontroller.linkdiscovery.Link;
import org.projectfloodlight.openflow.types.DatapathId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FordGraph{


    public static ArrayList<Integer>[] adjacencyList=null;
    public int noOfVertices;
    private HashMap<Integer,Integer> mapOfNodesKeepsIndex = new HashMap<Integer,Integer>();


    public FordGraph(Map<DatapathId, Set<Link>> links)
    {
        noOfVertices = links.size();
        HashMap<Integer,Integer> mapOfNodesKeepsIndex = new HashMap<Integer,Integer>();
        int indexOfThatNode = 0;
        for(DatapathId id : links.keySet())
        {
            for(Link link : links.get(id))
            {
                if( ! mapOfNodesKeepsIndex.containsKey((int)link.getDst().getLong()))
                {
                    mapOfNodesKeepsIndex.put((int)link.getDst().getLong(),indexOfThatNode);//id yi koyabiliriz ?
                    indexOfThatNode++;
                }
            }
        }
        setMapOfNodesKeepsIndex(mapOfNodesKeepsIndex);


        adjacencyList=(ArrayList<Integer>[])new ArrayList[noOfVertices];
        this.noOfVertices=noOfVertices;
        for(int i=1;i<noOfVertices+1;i++)
            adjacencyList[getMapOfNodesKeepsIndex().get(i)]=new ArrayList<Integer>();
    }

    public void addEdge(int u, int v)
    {
        if(adjacencyList[getMapOfNodesKeepsIndex().get(u)]==null)
            adjacencyList[getMapOfNodesKeepsIndex().get(u)]=new ArrayList<Integer>();

        if( ! adjacencyList[getMapOfNodesKeepsIndex().get(u)].contains(v))
        {
            adjacencyList[getMapOfNodesKeepsIndex().get(u)].add(v);
        }
    }

    /**
     *
     * @param u
     * @param v
     * To remove the edge from the graph
     */
    public void removeEdge(int u, int v)
    {
        int indexToBeRemoved=-1;
        ArrayList<Integer> edgeList=adjacencyList[getMapOfNodesKeepsIndex().get(u)];
        for(int i=0;i<adjacencyList[getMapOfNodesKeepsIndex().get(u)].size();i++)
        {
            int val=edgeList.get(i);
            if(val==v)
            {
                indexToBeRemoved=i;
            }
        }
        edgeList.remove(indexToBeRemoved);
    }

    /**
     * Method to verify whether u and v are neighbors
     * @param u
     * @param v
     * @return
     */
    public boolean isNeighbor(int u, int v)
    {
        if(adjacencyList[getMapOfNodesKeepsIndex().get(u)]==null)
            return false;
        return adjacencyList[getMapOfNodesKeepsIndex().get(u)].contains(v);

    }

    /**
     * Method to return the size of the graph
     * @return
     */
    public int size()
    {
        return adjacencyList.length;
    }
    /**
     *
     * @param u
     * @return
     * To return the outgoing edges for the given source
     */
    public ArrayList<Integer> getOutEdges(int u)
    {
        return adjacencyList[getMapOfNodesKeepsIndex().get(u)];
    }

    /**
     * Method to return the adjacency list
     * @return
     */
    public ArrayList<Integer>[] getAdjacencyList()
    {
        return adjacencyList;
    }

    public void printGraph()
    {
        ArrayList<Integer> edgeList;
        for(int i=1;i<=noOfVertices;i++)
        {
            edgeList=adjacencyList[getMapOfNodesKeepsIndex().get(i)];
            if(edgeList!=null)
            {
                for(int v : edgeList)
                    System.out.println("u : "+i+" v : "+v);
            }
        }
    }

    public HashMap<Integer, Integer> getMapOfNodesKeepsIndex() {
        return mapOfNodesKeepsIndex;
    }

    public void setMapOfNodesKeepsIndex(HashMap<Integer, Integer> mapOfNodesKeepsIndex) {
        this.mapOfNodesKeepsIndex = mapOfNodesKeepsIndex;
    }
}