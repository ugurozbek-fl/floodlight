package net.floodlightcontroller.topology.tunahan;

import net.floodlightcontroller.linkdiscovery.Link;
import net.floodlightcontroller.routing.BroadcastTree;
import org.projectfloodlight.openflow.types.DatapathId;

import java.util.Map;
import java.util.Set;

public class AlgorithmExecutor {

    public AlgorithmExecutor() {
    }

    public void runAlgorithms(DatapathId src, DatapathId dst, Map<Link, Integer> linkCost, Map<Link, Integer> linkBandWith, Map<DatapathId, Set<Link>> copyOfLinkDpidMap) {
        int intSrc = 2;
        int intDst = 18;
        Long baseSrc = Long.valueOf(intSrc);
        Long baseDst = Long.valueOf(intDst);
        if(src.getLong() == baseSrc && dst.getLong() == baseDst) {
            try {
                int normalizationType= 0; // 0 - one parameter, 1 feature scalling, 2 standard score
                DualAscentAlgorithm dualAscentAlgorithm = new DualAscentAlgorithm(normalizationType);
                BroadcastTree treeForDualAscent = dualAscentAlgorithm.findPath(copyOfLinkDpidMap, src, dst, linkCost, linkBandWith);

                BellmanFordAlgorithm bellmanFordAlgorithm = new BellmanFordAlgorithm(normalizationType);
                BroadcastTree   treeForBellmanFord = bellmanFordAlgorithm.findPath(copyOfLinkDpidMap, dst, linkCost ,true, linkBandWith);

                AuctionAlgorithm auctionAlgorithm = new AuctionAlgorithm(normalizationType);
                BroadcastTree treeForAuction = auctionAlgorithm.findPath(copyOfLinkDpidMap, src, dst, linkCost,  linkBandWith);

                DijkstraAlgorithm dijkstraAlgorithm= new DijkstraAlgorithm(normalizationType);
                BroadcastTree treeForDijkstra = dijkstraAlgorithm.findPath(copyOfLinkDpidMap, dst, linkCost , true ,linkBandWith);

                FordFulkersonAlgorithm fordAlgorithm = new FordFulkersonAlgorithm(normalizationType);
                BroadcastTree treeForFF = fordAlgorithm.findPath(copyOfLinkDpidMap, src, dst, linkCost, linkBandWith);

                new PrintingHelper().printingAllCosts(linkCost, linkBandWith , baseSrc, treeForDualAscent, treeForBellmanFord, treeForAuction, treeForDijkstra, treeForFF);

            }catch (Exception ex)
            {
                PrintingHelper.writeUsingFileWriter(ex.getStackTrace()[0].toString(),"ERROR_"+PrintingHelper.getFileNameAsDate());
            }
        }
    }
}
