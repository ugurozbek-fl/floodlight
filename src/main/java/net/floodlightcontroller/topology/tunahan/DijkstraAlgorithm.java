package net.floodlightcontroller.topology.tunahan;

import net.floodlightcontroller.linkdiscovery.Link;
import net.floodlightcontroller.routing.BroadcastTree;
import net.floodlightcontroller.topology.NodeDist;
import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import static net.floodlightcontroller.topology.TopologyInstance.MAX_PATH_WEIGHT;

public class DijkstraAlgorithm {

    public DijkstraAlgorithm(int normalizationType) {
        this.normalizationType = normalizationType;
    }

    private int normalizationType = 0;
    private static final Logger log = LoggerFactory.getLogger(DijkstraAlgorithm.class);

    public BroadcastTree findPath(Map<DatapathId, Set<Link>> links, DatapathId root, Map<Link, Integer> linkCost, boolean isDstRooted ,
                                  Map<Link, Integer> linkBandWith) {
        HashMap<DatapathId, Link> nexthoplinks = new HashMap<DatapathId, Link>(); // liste. her bir ID(node) ye karşılık next link verisi
        HashMap<DatapathId, Double> cost = new HashMap<>(); // liste. her bir node ID sine karşılık bir integer değer
        //links -> bir liste. her bir node ID sine karşılık link listesi.( bir link = bir bağlantı)
        int w;
        double wNormalized = 0;

        for (DatapathId node : links.keySet()) {// tüm nodları sonsuz yapıyor bi en başta
            nexthoplinks.put(node, null); // dönecek olan veriler nextHopLinks ve cost. İkisini de ilklendiriyor.
            cost.put(node, ((double)MAX_PATH_WEIGHT));
            //log.debug("Added max cost to {}", node);
        }

        HashMap<DatapathId, Boolean> seen = new HashMap<DatapathId, Boolean>(); // node ID sine karşılık boolean tutan liste
        PriorityQueue<NodeDist> nodeq = new PriorityQueue<NodeDist>(); // kuyruk veri yapısı. bir NodeDist =node ID ve int Distance bilgisi
        nodeq.add(new NodeDist(root, 0));//nodeq döngü ile döndüğümüz kuyruk veriyapısı. ilk başlangıç olarak root veriyoruz
        //UNSETTLED
        cost.put(root, Double.valueOf(0));

        while (nodeq.peek() != null) {
            NodeDist n = nodeq.poll();
            DatapathId cnode = n.getNode();
            int cdist = n.getDist();

            if (cdist >= MAX_PATH_WEIGHT) break;
            if (seen.containsKey(cnode)) continue;
            seen.put(cnode, true);

            if (links.get(cnode) == null) continue;
            for (Link link : links.get(cnode)) {
                DatapathId neighbor;
                if (isDstRooted == true) {
                    neighbor = link.getSrc();
                } else {
                    neighbor = link.getDst();
                }
                if (neighbor.equals(cnode)) continue;

                if (seen.containsKey(neighbor)) continue;
                if (linkCost == null || linkCost.get(link) == null) {
                    w = 1;
                } else {
                    w = linkCost.get(link);
                    if (normalizationType == 0) {
                        wNormalized = linkCost.get(link);  // no normalization
                    } else if (normalizationType == 1) {
                        double normalizedMinBandWidth = NormalizationManager.getNormalizedBandWidth(linkBandWith, link);//normalization 1
                        wNormalized = ((double) linkCost.get(link)) / (normalizedMinBandWidth); //normalization 1 and 2
                    } else if (normalizationType == 2) {
                        double normalizedMinBandWidth = NormalizationManager.StandardScoreShifting + NormalizationManager.getNormalizedBandWidth2(linkBandWith, link);//normalization 2
                        wNormalized = ((double) linkCost.get(link)) / (normalizedMinBandWidth); //normalization 1 and 2
                    }
                }
                int ndist = cdist + w; // the weight of the link, always 1 in current version of floodlight.
                double nDistNormalized = ((double)cdist) + wNormalized;
                log.debug("Neighbor: {}", neighbor);
                log.debug("Cost: {}", cost);
                log.debug("Neighbor cost:MAX_PATH_WEIGHT {}", cost.get(neighbor));
                if (nDistNormalized < cost.get(neighbor)) {
                    cost.put(neighbor, nDistNormalized);
                    nexthoplinks.put(neighbor, link);
                    NodeDist ndTemp = new NodeDist(neighbor, ndist);
                    nodeq.remove(ndTemp);
                    nodeq.add(ndTemp);

                }
            }
        }

        HashMap<DatapathId, Integer> costToReturn = new HashMap<>();
        for (DatapathId keyCost : cost.keySet()) {
            double keyVal = cost.get(keyCost);
            costToReturn.put(keyCost, (int) keyVal);
        }
        BroadcastTree ret = new BroadcastTree(nexthoplinks, costToReturn);

        return ret;
    }
}
