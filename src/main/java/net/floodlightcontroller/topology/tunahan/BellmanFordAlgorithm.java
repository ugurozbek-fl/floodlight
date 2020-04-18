package net.floodlightcontroller.topology.tunahan;

import net.floodlightcontroller.linkdiscovery.Link;
import net.floodlightcontroller.routing.BroadcastTree;
import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static net.floodlightcontroller.topology.TopologyInstance.MAX_PATH_WEIGHT;

public class BellmanFordAlgorithm {
    private int normalizationType = 0;
    private static final Logger log = LoggerFactory.getLogger(BellmanFordAlgorithm.class);

    public BellmanFordAlgorithm(int normalizationType) {
        this.normalizationType = normalizationType;
    }


    public BroadcastTree findPath(Map<DatapathId, Set<Link>> links, DatapathId root, Map<Link, Integer> linkCost,
                                     boolean isDstRooted, Map<Link, Integer> linkBandWith) {
        HashMap<DatapathId, Link> nexthoplinks = new HashMap<DatapathId, Link>();
        HashMap<DatapathId, Double> cost = new HashMap<DatapathId, Double>();
//        HashMap<DatapathId, Integer> costToReturn = new HashMap<>();
        double w = 0;
        // Step 1: Initialize distances from src to all other vertices as INFINITE
        for (DatapathId node : links.keySet()) {
            nexthoplinks.put(node, null);
            cost.put(node, Double.MAX_VALUE);
//            costToReturn.put(node, MAX_VALUE);
        }
        // Step 2: Relax all edges |V| - 1 times. A simple shortest path from src to any other vertex can
        // have at-most |V| - 1 edges
        DatapathId temp = root;
        DatapathId u, v;
        int a = links.keySet().size();
        cost.put(root, Double.valueOf(0));
//        costToReturn.put(root, 0);
        for (int k = 1; k < a; k++) {
            for (DatapathId node : links.keySet()) {
                for (Link link : links.get(node)) {
                    if(isDstRooted==true) {
                        v=link.getSrc();
                        u=link.getDst();
                    } else {
                        continue;
                    }
                    if (linkCost == null || linkCost.get(link) == null) {
                        w = 1;
                    } else {
                        if (normalizationType == 0) {
                            w = linkCost.get(link); // no normalization
                        } else if (normalizationType == 1) {
                            double normalizedMinBandWidth = NormalizationManager.getNormalizedBandWidth(linkBandWith, link);// norm1
                            w = ((double)linkCost.get(link)) / (normalizedMinBandWidth); //normalization 1 and 2
                        } else if (normalizationType == 2) {
                            double normalizedMinBandWidth = NormalizationManager.StandardScoreShifting+ NormalizationManager.getNormalizedBandWidth2(linkBandWith, link); //norm2
                            w = ((double)linkCost.get(link)) / (normalizedMinBandWidth); //normalization 1 and 2
                        }
                    }
                        if (cost.get(v) > cost.get(u) + w) {
                            cost.put(v, cost.get(u) + w);
                            nexthoplinks.put(v, link);
                        }
                    //Step 3--Negative cycle detection
                    if (cost.get(u) != MAX_PATH_WEIGHT && cost.get(u) + w < cost.get(v)) {
                        log.info("Graph contains negative weight cycle");
                    }

                }
            }
        }
        HashMap<DatapathId, Integer> costToReturn = new HashMap<>();
        for(DatapathId keyCost : cost.keySet())
        {
            double keyVal = cost.get(keyCost);
            costToReturn.put(keyCost , (int) keyVal);
        }
        BroadcastTree ret = new BroadcastTree(nexthoplinks, costToReturn);
        return ret;
    }
}
