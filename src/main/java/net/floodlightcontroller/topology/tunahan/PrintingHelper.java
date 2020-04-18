package net.floodlightcontroller.topology.tunahan;

import net.floodlightcontroller.linkdiscovery.Link;
import net.floodlightcontroller.routing.BroadcastTree;
import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PrintingHelper {

    private static final Logger log = LoggerFactory.getLogger(PrintingHelper.class);

    public void printingAllCosts(Map<Link, Integer> linkCost, Map<Link, Integer> linkBandWith, Long baseSrc, BroadcastTree treeForDualAscent, BroadcastTree treeForBellmanFord, BroadcastTree treeForAuction, BroadcastTree treeForDijkstra, BroadcastTree treeForFF) {

        StringBuffer result = new StringBuffer(getFileNameAsDate());
        String nl = System.getProperty("line.separator");
        result.append(nl);
        ArrayList<Long> arrayForAuctions = convertedPath(treeForAuction.getLinks(), baseSrc);
        ArrayList<Long> arrayForBellmanFord = convertedPath(treeForBellmanFord.getLinks(), baseSrc);
        ArrayList<Long> arrayForDijkstra = convertedPath(treeForDijkstra.getLinks(), baseSrc);
        ArrayList<Long> arrayForDualAscent = convertedPath(treeForDualAscent.getLinks(), baseSrc);
        ArrayList<Long> arrayForFordFulkerson = convertedPath(treeForFF.getLinks(), baseSrc);

        int intForDijkstra = getCostForGivenTree(arrayForDijkstra,linkCost);
        int intForBellmanFord = getCostForGivenTree(arrayForBellmanFord,linkCost);
        int intForAuction = getCostForGivenTree(arrayForAuctions,linkCost);
        int intForDualAscent = getCostForGivenTree(arrayForDualAscent,linkCost);
        int intForFordFulkerson = getCostForGivenTree(arrayForFordFulkerson,linkCost);
        int localOrt = (intForAuction+intForBellmanFord+intForDijkstra+intForDualAscent+intForFordFulkerson) / 5;
//
        result.append("AUCTION ").append(arrayForAuctions).append(" " + intForAuction);
        result.append(nl);

        result.append("BELLMAN ").append(arrayForBellmanFord).append(" " +intForBellmanFord);
        result.append(nl);

        result.append("DIJKSTRA ").append(arrayForDijkstra).append(" " +intForDijkstra);
        result.append(nl);

        result.append("DUAL ").append(arrayForDualAscent).append(" " +intForDualAscent);
        result.append(nl);

        result.append("FORD ").append(arrayForFordFulkerson).append(" " +intForFordFulkerson);
        result.append(nl);

        result.append("MEAN = "+localOrt);
        result.append(nl);

        result.append("-COSTS-");
        result.append(printCosts(linkCost));
        result.append(nl);

        result.append("-BANDWIDTHS-");
        result.append(printCosts(linkBandWith));
        result.append(nl);
        log.info("---END---");
        writeUsingFileWriter(result.toString(),getFileNameAsDate());
    }

    public static void writeUsingFileWriter(String data,String fileName) {
        File file = new File(fileName+".txt");
        FileWriter fr = null;
        try {
            fr = new FileWriter(file, true);
            fr.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //close resources
            try {
                fr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getFileNameAsDate()
    {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        return dtf.format(now);
    }

    private  ArrayList<Long> convertedPath(HashMap<DatapathId, Link> links, Long baseSrc) {
        long localFrom = 0;
        long localTo = 0;
        ArrayList<Long> fin = new ArrayList<Long>();
        fin.add(baseSrc);
        while (true) {
            localFrom = fin.get(fin.size() - 1);
            localTo = getToLongForGivenFromLong(links, localFrom);
            if (localTo == -1) break;
            fin.add(localTo);
        }
        return fin;
    }

    private long getToLongForGivenFromLong(HashMap<DatapathId, Link> list, Long givenFrom) {
        Link tempLink;
        DatapathId idToBeSearched = null;
        for (DatapathId item : list.keySet()) {
            tempLink = list.get(item);
            if (tempLink != null && tempLink.getSrc() != null && tempLink.getSrc().getLong() == givenFrom) {
                idToBeSearched = tempLink.getDst();
            }
        }
        return idToBeSearched == null ? -1 : idToBeSearched.getLong();
    }

    private int getCostForGivenTree(ArrayList<Long> listX, Map<Link, Integer> linkCost) {
        int val = 0;
        long firstNode = listX.get(0);
        for(int i=1;i<listX.size();i++)
        {
            val = val + getLinkCost(linkCost,firstNode,listX.get(i));
            firstNode = listX.get(i);
        }
        return val;
    }

    private Integer getLinkCost(Map<Link, Integer> linkCost, Long src, Long dst) {
        Integer returnVal = 0;
        try {
            for (Link link : linkCost.keySet()) {
                if (link !=null && link.getSrc().getLong() == src && link.getDst().getLong() == dst) {
                    if(linkCost.get(link) != null)
                    {
                        returnVal = linkCost.get(link);
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            returnVal = 0;
        }
        return returnVal;
    }

    private String printCosts(Map<Link, Integer> costs) {
        String result = "";
        int i = 0;
        for(Link link : costs.keySet())
        {
            result = result + "\n"+ link.getSrc().getLong()+"-"+ link.getDst().getLong() +"-"+ costs.get(link);
        }
        return result;
    }

    private DatapathId getFinalNodeForLogging(HashMap<DatapathId, Link> list) {
        Link tempVal;
        DatapathId finalNode = null;
        for (DatapathId item : list.keySet()) {
            tempVal = list.get(item);
            if (tempVal == null) {
                finalNode = item;
            } else if (tempVal.getDst() == null) {
                finalNode = tempVal.getDst();
            }
        }
        return finalNode;
    }


    private DatapathId getFirstNodeForLogging(HashMap<DatapathId, Link> list, Long startNode) {
        Link tempLink;
        DatapathId firstNode = null;
        for (DatapathId item : list.keySet()) {
            tempLink = list.get(item);
            if (tempLink == null) continue;
            if (tempLink.getSrc().getLong() == startNode) {
                firstNode = tempLink.getSrc();
                break;
            }
        }
        return firstNode;
    }
}
