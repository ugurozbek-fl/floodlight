package net.floodlightcontroller.topology.tunahan;

import net.floodlightcontroller.linkdiscovery.Link;
import net.floodlightcontroller.routing.BroadcastTree;
import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class FordFulkersonAlgorithm {
    private int normalizationType = 0;
    private static final Logger log = LoggerFactory.getLogger(FordFulkersonAlgorithm.class);
    private HashMap<Link, Integer> virtCost = new HashMap<Link, Integer>();
    DatapathId src, dst = null;
    Map<DatapathId, Set<Link>> links;
    Map<Link, Integer> linkCost;
    int siwCount = 0;
    HashSet<Long> requiredVertices = new HashSet<Long>();
    Map<Link, Integer> linkBandWith;

    public FordFulkersonAlgorithm(int normalizationType) {
        this.normalizationType = normalizationType;
    }

    public BroadcastTree findPath(Map<DatapathId, Set<Link>> links, DatapathId src, DatapathId dst, Map<Link, Integer> linkCost,Map<Link, Integer> linkBandWith) {
        this.linkCost = linkCost;
        this.src = src;
        this.dst = dst;
        requiredVertices.add(this.dst.getLong());
        this.links = links;
        siwCount = links.size();
        this.linkBandWith = linkBandWith;

        FordGraph graph = constructGraph(links, src, dst, linkCost, links.size());
        ArrayList<ArrayList<Integer>> shortestPathList = bfsAllPathMatrix(graph, (int) src.getLong(), (int) dst.getLong());
        ArrayList<ArrayList<Link>> linkMatrix = convertToLinks(links, shortestPathList);
        ArrayList<Link> bestOption = getBestOptionInMatrix(linkMatrix);
        HashMap<DatapathId, Link> convertedSolution = convertSolutionForBroadCastTree(bestOption);
        HashMap<DatapathId, Integer> convertedCost = convertCostForBroadCasTree(bestOption);
        ///////////////////////////
        BroadcastTree ret = new BroadcastTree(convertedSolution, convertedCost);
        return ret;
    }

    private HashMap<DatapathId, Integer> convertCostForBroadCasTree(ArrayList<Link> sol) {
        HashMap<DatapathId, Integer> returnCost = new HashMap<>();
        for (Link link : sol) {
            if (link == null) continue;
            returnCost.put(link.getSrc(), getLinkCost(this.linkCost, link));
        }
        return returnCost;
    }

    private HashMap<DatapathId, Link> convertSolutionForBroadCastTree(ArrayList<Link> sol) {
        HashMap<DatapathId, Link> returnHashMap = new HashMap<>();
        for (Link link : sol) {
            if (link == null) continue;
            try {
                if (!returnHashMap.containsKey(link.getSrc())) {
                    returnHashMap.put(link.getSrc(), link);
                }
            } catch (Exception ex) {
                System.out.println("errr convertSolutionForBroadCastTree " + ex.getMessage());
            }
        }
        return returnHashMap;
    }

    private ArrayList<Link> getBestOptionInMatrix(ArrayList<ArrayList<Link>> linkMatrix) {
        double minValue = Long.MAX_VALUE;
        int minPrefIndex = 0;
        for (int j = 0; j < linkMatrix.size(); j++) {
            ArrayList<Link> onePath = linkMatrix.get(j);
            double localCostValue = 0;
            for (Link oneLink : onePath) {
                if (oneLink != null && linkCost.get(oneLink) != null) {
                    if (normalizationType == 0) {
                        localCostValue += this.linkCost.get(oneLink);
                    } else if (normalizationType == 1) {
                        localCostValue += NormalizationManager.getMixedCost(oneLink, this.linkCost, this.linkBandWith);
                    } else if (normalizationType == 2) {
                        localCostValue += NormalizationManager.getMixedCost2(oneLink, this.linkCost, this.linkBandWith);
                    }
                }
            }
            if (localCostValue < minValue) {
                minValue = localCostValue;
                minPrefIndex = j;
            }
        }
        return linkMatrix.get(minPrefIndex);
    }

    private ArrayList<ArrayList<Link>> convertToLinks(Map<DatapathId, Set<Link>> links, ArrayList<ArrayList<Integer>> shortestPathList) {
        ArrayList<ArrayList<Link>> returnMatix = new ArrayList<>();
        for (ArrayList<Integer> onePath : shortestPathList) {
            ArrayList<Link> oneLinkSeries = new ArrayList<>();
            for (int i = 0; i < onePath.size(); i++) {
                int currentNode = onePath.get(i);
                int theNextNode = 0;
                if (i < onePath.size() - 1) {
                    theNextNode = onePath.get(i + 1);
                } else {
                    break;
                }
                Link theLink = findLinkInLinks(links, currentNode, theNextNode);
                oneLinkSeries.add(theLink);
            }
            returnMatix.add(oneLinkSeries);
        }
        return returnMatix;
    }

    private Link findLinkInLinks(Map<DatapathId, Set<Link>> links, int currentNode, int theNextNode) {
        Link returnLink = new Link();
        for (DatapathId id : links.keySet()) {
            if (((int) id.getLong()) == currentNode) {
                Set<Link> linkSet = links.get(id);
                if (linkSet == null) continue;
                for (Link link : linkSet) {
                    if (((int) link.getDst().getLong()) == theNextNode) {
                        returnLink = link;
                    }
                }
            } else {
                continue;
            }
        }
        return returnLink;
    }


    private Set<Link> getListOfEnteringLinksToGivenNode(Long n) { //---> node
        HashSet<Link> linkSet = new HashSet<>();
        try {
            return links.get(DatapathId.of(n)).stream().filter(link -> link.getDst().getLong() == n).collect(Collectors.toSet());
        } catch (Exception ex) {
            System.out.println("errr getListOfEnteringLinksToGivenNode " + ex.getMessage());
        }
        return linkSet;
    }

    private Integer getCost(Link a) {
        Integer i = virtCost.get(a);
        if (i == null)
            i = getLinkCost(this.linkCost, a);// instance.getIntCost(a);
        return i;
    }

    private int getLinkCost(Map<Link, Integer> linkCost, Link theLinkNormal) {
        if (theLinkNormal == null) return 0;
        int returnVal = 0;
        try {
            returnVal = linkCost.get(theLinkNormal);
        } catch (Exception ex) {
            returnVal = 1;
        }
        return returnVal == 0 ? 1 : returnVal;
    }

    private FordGraph constructGraph(Map<DatapathId, Set<Link>> links, DatapathId src, DatapathId dst, Map<Link, Integer> linkCost, int switchCount) {
        FordGraph graph = new FordGraph(links);
        for (DatapathId theID : links.keySet()) {
            for (Link theLink : links.get(theID)) {
                graph.addEdge((int) theLink.getSrc().getLong(), (int) theLink.getDst().getLong());
            }
        }
        return graph;
    }

    ///////////////

    public static ArrayList<ArrayList<Integer>> bfsAllPathMatrix(FordGraph graph, int src, int dst) {
        Queue<ArrayList<Integer>> q = new LinkedList<>();
        ArrayList<Integer> path = new ArrayList<>();
        ArrayList<ArrayList<Integer>> matrix = new ArrayList<>();
        path.add(src);
        q.add(path);
        while (!q.isEmpty()) {
            path = q.poll();
            int last = path.get(path.size() - 1);
            if (last == dst && Math.random() < 0.21) {
                matrix.add(new ArrayList<>(path));
                if (matrix.size() == 50) {
                    break;
                }
//                  log.info("{}",path); log.info("{}",matrix);
            }
            for (int i = 0; i < graph.getOutEdges(last).size(); i++) {
                if (isNotVisited(graph.getOutEdges(last).get(i), path)) {
                    ArrayList<Integer> newPath = new ArrayList<>(path);
                    newPath.add(graph.getOutEdges(last).get(i));
                    q.add(newPath);
                }
            }
        }

        return matrix;
    }

    static boolean isNotVisited(int i, ArrayList<Integer> path) {
//        return path.contains(i);
        for (int currentNode : path) {
            if (i == currentNode) {
                return false;
            }
        }
        return true;
    }
}
