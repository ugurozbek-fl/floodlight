package net.floodlightcontroller.topology.tunahan;

import net.floodlightcontroller.linkdiscovery.Link;
import net.floodlightcontroller.routing.BroadcastTree;
import net.floodlightcontroller.topology.TopologyInstance;
import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AuctionAlgorithm {

    Map<Link, Integer> linkBandWith;
    private int normalizationType = 0;

    public AuctionAlgorithm(int normalizationType) {
        this.normalizationType = normalizationType;
    }

    public BroadcastTree findPath(Map<DatapathId, Set<Link>> links, DatapathId from, DatapathId toNode, Map<Link, Integer> linkCost, Map<Link, Integer> linkBandWith) {
        HashMap<DatapathId, Link> nexthoplinks = new HashMap<DatapathId, Link>();
        HashMap<DatapathId, Integer> cost = new HashMap<DatapathId, Integer>();
        this.linkBandWith= linkBandWith;
        Logger log = LoggerFactory.getLogger(TopologyInstance.class);
        if(from.getLong() == toNode.getLong())
        {
           return new BroadcastTree(nexthoplinks, cost);
        }
        long[] pNodeVector = new long[links.size()];
        for (int i = 0 ; i < pNodeVector.length ; i++) {
            pNodeVector[i] = 0;
        }
        //initializing
        HashMap<Long,Integer> mapOfNodesKeepsIndex = new HashMap<Long,Integer>();
        int indexOfThatNode = 0;
        for(DatapathId id : links.keySet())
        {
            for(Link link : links.get(id))
            {
                long tempDst = link.getDst().getLong();
                long tempSrc = link.getSrc().getLong();
                if( ! mapOfNodesKeepsIndex.containsKey(tempDst))
                {
                    mapOfNodesKeepsIndex.put(tempDst,indexOfThatNode);
                    indexOfThatNode++;
                }
                if( ! mapOfNodesKeepsIndex.containsKey(tempSrc))
                {
                    mapOfNodesKeepsIndex.put(tempSrc,indexOfThatNode);
                    indexOfThatNode++;
                }

            }
        }

        DatapathId currentNode = from;
        int currentInd = mapOfNodesKeepsIndex.get(from.getLong());
        while (true) {
            int longToFindRoute = Integer.MAX_VALUE;
            Link nexLink = null;
            int nexLinkCost = 0;
            Set<Link> linksForGivenNode = links.get(currentNode);
            for (Link link : linksForGivenNode) {//local dongusu
                int local_calculatedNextNodeValue;
                int local_calculatedWeightToNextNode;
                if (link.getSrc() != currentNode) continue;
                local_calculatedNextNodeValue = Math.toIntExact(pNodeVector[mapOfNodesKeepsIndex.get(link.getDst().getLong())]);
                local_calculatedWeightToNextNode =  getLinkCost(linkCost,link);  // linkCost.get(link);
                int aggregatedInt = local_calculatedNextNodeValue+local_calculatedWeightToNextNode;
                if (nexLink == null ||checkIfLinkHasLessCost(longToFindRoute,nexLink, aggregatedInt,link)) {
                    longToFindRoute = local_calculatedNextNodeValue + local_calculatedWeightToNextNode;
                    nexLink = link;
                    nexLinkCost = getLinkCost(linkCost,link);
                }
            }
            if (pNodeVector[currentInd] < longToFindRoute) {
                //contractPath
                pNodeVector[currentInd] = longToFindRoute;
                DatapathId getLastFrom = getLastFrom(currentNode, nexthoplinks);
                if (getLastFrom != null && !getLastFrom.equals(from)) { // do not remove the starting point
                    currentNode = getLastFrom;
                    nexthoplinks.remove(currentNode);
                    currentInd = mapOfNodesKeepsIndex.get(currentNode.getLong());
                }
            } else {
                //extend Path
                nexthoplinks.put(currentNode, nexLink);
                cost.put(currentNode, nexLinkCost);
                if (nexLink.getDst() == toNode) {
                    break;
                }
                currentNode = nexLink.getDst();//it could be either previous node or extended node
                currentInd = mapOfNodesKeepsIndex.get(currentNode.getLong());
            }
        }
        BroadcastTree ret = new BroadcastTree(nexthoplinks, cost);

        return ret;
    }

    private DatapathId getDatapathId(HashMap<DatapathId, Link> nexthoplinks, DatapathId currentNode) {
        DatapathId idToBeDeleted =null;
        for (DatapathId idFor : nexthoplinks.keySet()) {
            Link linkFor = nexthoplinks.get(idFor);
            if (linkFor.getDst().getLong() == currentNode.getLong()) {
                idToBeDeleted = idFor;
                break;
            }
        }
        return idToBeDeleted;
    }

    private boolean checkIfLinkHasLessCost(int longToFindRoute, Link cuurentlyMinLink, int local_aggregated_values, Link currentLink) {
        if (normalizationType == 0) {
            return longToFindRoute > local_aggregated_values;
        } else if (normalizationType == 1) {
            double normalizedMinBandWidth = NormalizationManager.getNormalizedBandWidth(this.linkBandWith, cuurentlyMinLink);
            double normalizedCurrentBandWidth = NormalizationManager.getNormalizedBandWidth(this.linkBandWith, currentLink);
            return (((double) longToFindRoute) * normalizedCurrentBandWidth) > (((double) local_aggregated_values) * normalizedMinBandWidth);
        } else if (normalizationType == 2) {
            double normalizedMinBandWidth2 = NormalizationManager.StandardScoreShifting + NormalizationManager.getNormalizedBandWidth2(this.linkBandWith, cuurentlyMinLink);
            double normalizedCurrentBandWidth2 = NormalizationManager.StandardScoreShifting + NormalizationManager.getNormalizedBandWidth2(this.linkBandWith, currentLink);
            return (((double) longToFindRoute) * normalizedCurrentBandWidth2) > (((double) local_aggregated_values) * normalizedMinBandWidth2);
        }
        return true;
    }

    private DatapathId getLastFrom(DatapathId id, HashMap<DatapathId, Link> nexthoplinks)
    {
//        System.out.println(generalCounter++ + " -  " +id.toString() + " (getLong)");
        DatapathId returnVal = null;
        //returnVal = getDatapathId(nexthoplinks, id, returnVal);
        for(DatapathId localID : nexthoplinks.keySet())
        {
            Link localLink = nexthoplinks.get(localID);
            if(localLink.getDst().getLong() == id.getLong())
            {
                returnVal= localID;
                break;
            }
        }
//        if(returnVal == null)
//        {
//            System.out.println(id + JSONObject.toJSONString(nexthoplinks));
//        }
        return returnVal;
    }

//    private DatapathId getLastFrom(DatapathId id, HashMap<DatapathId, Link> nexthoplinks)
//    {
//        DatapathId returnVal = null;
//        for(DatapathId localID : nexthoplinks.keySet())
//        {
//            Link localLink = nexthoplinks.get(localID);
//            if(localLink.getDst().getLong() == id.getLong())
//            {
//                returnVal= localID;
//                break;
//            }
//        }
//        return returnVal;
//    }
    private Integer getLinkBandwidth(Link linkMin) {
        //if it does not work than make is working by using loop
        return this.linkBandWith.get(linkMin);
    }
    private static int getLinkCost(Map<Link, Integer> linkCost, Link theLinkNormal) {
        if(theLinkNormal == null) return 0;
        int returnVal = 0;
        try {
            returnVal = linkCost.get(theLinkNormal);
        } catch (Exception ex) {
            returnVal = 1;
        }
        return returnVal == 0 ? 1 : returnVal;
    }
}
