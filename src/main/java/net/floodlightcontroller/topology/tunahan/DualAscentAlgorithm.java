package net.floodlightcontroller.topology.tunahan;

import net.floodlightcontroller.linkdiscovery.Link;
import net.floodlightcontroller.routing.BroadcastTree;
import net.floodlightcontroller.topology.tunahan.dualascent.Couple;
import net.floodlightcontroller.topology.tunahan.dualascent.Graph;
import org.projectfloodlight.openflow.types.DatapathId;

import java.util.*;
import java.util.stream.Collectors;

public class DualAscentAlgorithm {

    private int normalizationType = 0;
    private HashMap<Long, Link> arcOfNode = new HashMap<Long, Link>();
    private HashMap<Link, Integer> virtCost = new HashMap<Link, Integer>();
    LinkedList<Couple<Long, HashSet<Long>>> contractions = new LinkedList<>();
    Graph gp;
    DatapathId src, dst = null;
    private HashMap<Link, Link> arcContractionLink = new HashMap<Link, Link>();
    Map<DatapathId, Set<Link>> links;
    HashMap<Long, Integer> nodeIndexes = new HashMap<Long, Integer>();
    boolean[][] connections;
    HashMap<Link, Integer> reducedCost;
    Long tempDualCost = 0L;
    Map<Link, Integer> linkCost;
    int siwCount = 0;

    private long maxId = -1;
    protected HashSet<Link> treeHash;
    protected Integer cost;
    protected Double dualCost;
    HashSet<Long> requiredVertices = new HashSet<Long>();
    Map<Link, Integer> linkBandWith;

    public DualAscentAlgorithm(int normalizationType) {
        this.normalizationType = normalizationType;
    }

    public BroadcastTree findPath(Map<DatapathId, Set<Link>> links, DatapathId src, DatapathId dst, Map<Link, Integer> linkCost, Map<Link, Integer> linkBandWith) {
        this.linkCost = linkCost;
        reducedCost = new HashMap<Link, Integer>(this.linkCost);
        this.src = src;
        this.dst = dst;
        requiredVertices.add(this.dst.getLong());
        gp = new Graph(links, false);
        this.links = links;
        this.siwCount = links.size();
        connections = initWithFalse(links.size());
        this.linkBandWith = linkBandWith;
        int index = 0;
        for (DatapathId node : links.keySet()) {
            connections[index][index] = true;
            nodeIndexes.put(node.getLong(), index);
            index++;
        }

        //burada iki degisken var aslinda. h ve gp. yeni noktalari gp ye ekleyerek gidiyor h i ise edit cost yaparken kullanıyor
        //ayrica h ikincil degisken. bir dugun gp ye eklendikten sonra h yeniden hesaplanir.
        HashSet<Long> tempGroup = eveluateRoot(gp); // step 1
        while (tempGroup != null) {
            //burada gp de bir alt liste h da bir alt liste gp>h. debug ederek farkını anla ve yeni tarafa ekle. -

            Link tempLink = findMinLink(gp, tempGroup); // (i*,j*) Find the minimum cost arc entering h which is not in gp.
            editCosts(tempGroup, tempLink); // Step 2 h uzerinden giden bir hesaplama. h in her bir elemanına giren okları alıp bunlarin rc
            //deki degerini aliyor, giren arc in degerini matematiksel olarak çıkarıyor ve guncellemis oluyor
            addLinkToSolution(gp, tempLink); // step 3 a gp ye ekleniyor
            // Back to step 1
            tempGroup = eveluateRoot(gp); // gelistirilmis gp uzerinden h i yeniden olusturuyoruz
        }



        setSolution(gp);
        HashMap<DatapathId, Link> convertedSolution = convertSolutionForBroadCastTree(treeHash);
        HashMap<DatapathId, Integer> convertedCost = convertCostForBroadCasTree(treeHash);
        BroadcastTree ret = new BroadcastTree(convertedSolution, convertedCost);
        return ret;
//        return arborescence;//todoooo 19 kasım salı
    }

    private Link findMinLink(Graph gp, HashSet<Long> h) {

        Link linkMin = null;
        for (Set<Link> linksOfCertainNode : this.links.values()) {
            for (Link certainLinkOfCertainNode : linksOfCertainNode) {
                if (h.contains(certainLinkOfCertainNode.getDst().getLong())) {
                    boolean isInGP = gpContainsThisLink(gp, certainLinkOfCertainNode);
                    if (isInGP) continue;
                    if (linkMin == null || checkGivenCertainNodeIsMinimum(linkMin, certainLinkOfCertainNode)) {
                        linkMin = certainLinkOfCertainNode;
                    }
                }
            }
        }
        if(linkMin == null)
        {
            System.out.println("LinkMin shoud not be null");
        }
        return linkMin;
    }

    private boolean checkGivenCertainNodeIsMinimum(Link linkMin, Link certainLinkOfCertainNode) {
        if (normalizationType == 0) {
            return reducedCost.get(linkMin) > reducedCost.get(certainLinkOfCertainNode);
        } else if (normalizationType == 1) {
            double normalizedLinkMin = NormalizationManager.getMixedCost(linkMin, reducedCost, this.linkBandWith);
            double normalizedCurrent = NormalizationManager.getMixedCost(certainLinkOfCertainNode, reducedCost, this.linkBandWith);
            return normalizedLinkMin > normalizedCurrent;
        } else if (normalizationType == 2) {
            double normalizedLinkMin = NormalizationManager.getMixedCost2(linkMin, reducedCost, this.linkBandWith);
            double normalizedCurrent = NormalizationManager.getMixedCost2(certainLinkOfCertainNode, reducedCost, this.linkBandWith);
            return normalizedLinkMin > normalizedCurrent;
        }
        return true;
    }

    private Integer getLinkBandwidth(Link linkMin) {
        //if it does not work than make is working by using loop
        return this.linkBandWith.get(linkMin);
    }

    private boolean gpContainsThisLink(Graph gp, Link linkOfTempNode) {
        return gp.links.values().stream().anyMatch(setLinkGP -> { //ONLY FOR GP KONTROLÜ
            for (Link li : setLinkGP) {
                if (li.getDst().getLong() == linkOfTempNode.getDst().getLong() && li.getSrc().getLong() == linkOfTempNode.getSrc().getLong()) {
                    return true;
                }
            }
            return false;
        });
    }

    private void editCosts(HashSet<Long> h, Link link) {

        int minCost = 1;
        try {
            minCost = reducedCost.get(link);
        }catch (Exception ex)
        { }
        tempDualCost += Long.valueOf(minCost);

        for (Long n : h) {
            Set<Link> listOfEnteringLinksToGivenNode = getListOfEnteringLinksToGivenNode(n);
            for (Link itLink : listOfEnteringLinksToGivenNode) {
                if (h.contains(itLink.getSrc().getLong()))
                    continue;
                int cost = reducedCost.get(itLink);
                reducedCost.put(itLink, cost - minCost);
            }
        }
    }

    private void addLinkToSolution(Graph gp, Link link) {
        Long lngSrc = link.getSrc().getLong();
        Long lngDst = link.getDst().getLong();
        Set<Link> a = gp.links.get(DatapathId.of(lngSrc));
        a.add(link);
        int iu = nodeIndexes.get(lngSrc);
        int iv = nodeIndexes.get(lngDst);

        int s = this.links.size();
        for (int i = 0; i < s; i++)
            for (int j = 0; j < s; j++)
                if (areConnected(i, iu) && areConnected(iv, j))
                    connections[i][j] = true;
    }

    private HashSet<Long> eveluateRoot(Graph gp) {
        Iterator requiredVerticesIterator = requiredVertices.iterator();
        Long n = null;
        HashSet<Long> rootComponent = new HashSet<Long>();
        int ir = nodeIndexes.get(this.src.getLong());

        // Find node for which neither root nor any other node dangles from.
        boucle1:
        while (requiredVerticesIterator.hasNext()) {
            rootComponent.clear();
            Long n1 = (Long) requiredVerticesIterator.next();
            if (n1.equals(this.src.getLong()))
                continue;

            int i1 = nodeIndexes.get(n1);
            if (areConnected(ir, i1))
                continue;

            rootComponent.add(n1);
            Iterator requiredVerticesIterator2 = requiredVertices.iterator();

            while (requiredVerticesIterator2.hasNext()) {
                Long n2 = (Long) requiredVerticesIterator2.next();
                if (n2.equals(this.src.getLong()) || n2.equals(n1))
                    continue;

                int i2 = nodeIndexes.get(n2);
                boolean con21 = areConnected(i2, i1);
                boolean con12 = areConnected(i1, i2);
                if (con21 && !con12)
                    continue boucle1;
                else if (!con21)
                    continue;
                rootComponent.add(n2);
            }

//            requiredVerticesIterator2 = requiredVertices.iterator();
            requiredVerticesIterator2 = gp.links.keySet().iterator();
            while (requiredVerticesIterator2.hasNext()) {
                DatapathId n2 = (DatapathId) requiredVerticesIterator2.next();//check datapathid long oluyor mu
                if (n2.getLong() == this.src.getLong() || rootComponent.contains(n2))
                    continue;
                int i2 = nodeIndexes.get(n2.getLong());
                if (areConnected(i2, i1))
                    rootComponent.add(n2.getLong());
            }

            n = n1;
            break;
        }

        if (n == null)
            return null;

        return rootComponent;
    }

    private boolean areConnected(int i1, int i2) {
        return this.connections[i1][i2];
    }

    private boolean[][] initWithFalse(int switchCount) {
        connections = new boolean[switchCount][switchCount];
        for (boolean[] column : connections) {
            Arrays.fill(column, false);
        }
        return connections;
    }

    private Set<Link> getListOfEnteringLinksToGivenNode(Long n) {
        HashSet<Link> linkSet = new HashSet<>();
        try{
            return links.get(DatapathId.of(n)).stream().filter(link -> link.getDst().getLong() == n).collect(Collectors.toSet());
        }catch (Exception ex)
        {
            System.out.println("errr getListOfEnteringLinksToGivenNode "+ex.getMessage());
        }
        return linkSet;
    }

    private void setSolution(Graph gp) {

        int ir = nodeIndexes.get(this.src.getLong());

        HashSet<Long> toRemove = new HashSet<Long>();
        for (DatapathId vertice : gp.links.keySet()) {
            int index = nodeIndexes.get(vertice.getLong());
            if (!areConnected(ir, index))
                toRemove.add(vertice.getLong());
        }
        for (Long n : toRemove)
            gp.removeVertice(n);


        HashSet<Link> solution = contradicCycles(gp);

        pruneTree(solution);

        treeHash = solution;
        if(cost == null) cost = 0;
        for (Link a : solution) {
            if (a != null) cost += getLinkCost(this.linkCost,a);// cost bu
        }
        dualCost = tempDualCost.doubleValue();
    }

    private HashMap<DatapathId, Integer> convertCostForBroadCasTree(HashSet<Link> sol) {
        HashMap<DatapathId, Integer> returnCost = new HashMap<DatapathId, Integer>();
        for(Link link : sol)
        {
            if(link == null) continue;
            returnCost.put(link.getSrc(),getLinkCost(this.linkCost,link));
        }
        return returnCost;
    }

    private HashMap<DatapathId, Link> convertSolutionForBroadCastTree(HashSet<Link> sol) {
        HashMap<DatapathId, Link> returnHashMap = new HashMap<DatapathId, Link>();
        for (Link link : sol) {
            if(link == null) continue;
            try {
                if (!returnHashMap.containsKey(link.getSrc())) {
                    returnHashMap.put(link.getSrc(), link);
                }
            } catch (Exception ex) {
                System.out.println("errr convertSolutionForBroadCastTree "+ex.getMessage());
            }
        }
        return returnHashMap;
    }

    private HashSet<Link> contradicCycles(Graph gp) {

        HashMap<Integer, Link> arcOfNode = new HashMap<Integer, Link>();
        LinkedList<Couple<Integer, HashSet<Integer>>> contractions = new LinkedList<Couple<Integer, HashSet<Integer>>>();
        HashMap<Link, Link> arcContractionLink = new HashMap<Link, Link>();
        HashMap<Link, Integer> virtCost = new HashMap<Link, Integer>();

        HashSet<Link> h = new HashSet<Link>();
        markEdges(h);
        HashSet<Long> cycle;
        while (true) {

            cycle = findCycle(h, links);
            if (cycle == null)
                break;

            contractCycle(cycle);

            h.clear();
            markEdges(h);
        }

        returnCyclesBack(h);
        treeHash = h;
        return treeHash;
    }

    private HashSet<Long> findCycle(HashSet<Link> h, Map<DatapathId, Set<Link>> links) {
        Graph g = gp.getInducedGraphFromArc(h, links);
        ArrayList<Long> ar = g.getOneDirectedCycle();
        if (ar == null)
            return null;
        HashSet<Long> cycle = new HashSet<Long>();
        for (Long v : ar)
            cycle.add(v);
        return cycle;
    }

    private void markEdges(HashSet<Link> h) {
        for (DatapathId v : this.gp.getLinksWithNoRemoved().keySet()) {
            maxId = Math.max(maxId, v.getLong() + 1);
            if (v.getLong() == this.src.getLong())
                continue;
            markEdge(h, v.getLong());
        }
    }

    private void markEdge(HashSet<Link> h, long v) {
        Link b = Collections.min(this.gp.getInputArcs(v), comp);
        h.add(b);
        arcOfNode.put(v, b);
    }

    private void pruneTree(HashSet<Link> sol) {
        // Remove useless arcs
        boolean doItAgain;
        do {
            doItAgain = false;
            Iterator<Link> it = sol.iterator();
            while (it.hasNext()) {
                Link a = it.next();
                if (a == null) continue;
                Long v = a.getDst().getLong();

                if (requiredVertices.contains(v))
                    continue;

                boolean leaf = true;
                Set<Link> setForIterator = new HashSet<Link>();
                for (Link tempLink : this.links.get(DatapathId.of(v))) {
                    if (tempLink.getSrc().getLong() == v) {
                        setForIterator.add(tempLink);
                    }
                }
                Iterator<Link> it2 = setForIterator.iterator();

                while (it2.hasNext()) {
                    Link b = it2.next();
                    if (sol.contains(b)) {
                        leaf = false;
                        break;
                    }
                }

                if (leaf) {
                    it.remove();
                    doItAgain = true;
                }
            }
        } while (doItAgain);
    }

    private void contractCycle(HashSet<Long> cycle) {
        try {
            gp.addVertice(maxId, links.get(DatapathId.of(maxId)));//checkkkkkkk
        } catch (Exception ex) {
            gp.addVertice(maxId, null);
            System.out.println("errr contractCycle "+ex.getMessage());
        }
        long contractNode = maxId;
        maxId++;

        setContractionInputs(contractNode, cycle);
        setContractionOutputs(contractNode, cycle);

        removeCycle(cycle);

        contractions.addFirst(new Couple<Long, HashSet<Long>>(contractNode, cycle));
    }

    private void removeCycle(HashSet<Long> cycle) {
        for (Long n : cycle)
            gp.virtuallyRemoveVertice(n);

    }

    private void setContractionInputs(Long contractNode, HashSet<Long> cycle) {
        HashMap<Long, Link> inputs = new HashMap<Long, Link>();
        HashMap<Long, Integer> inputsC = new HashMap<Long, Integer>();

        Link a;
        Iterator<Link> it;
        for (Long n : cycle) {
            Link aon = arcOfNode.get(n);
            it = gp.getInputArcsIterator(n);
            while (it.hasNext()) {
                a = it.next();
                Long input = a.getSrc().getLong();
                if (cycle.contains(input))
                    continue;
                int relCostA = getCost(a) - getCost(aon);
                Link b = inputs.get(input);
                if (b != null) {
                    long relCostB = inputsC.get(input);
                    if (relCostA < relCostB) {
                        inputs.put(input, a);
                        inputsC.put(input, relCostA);
                    }
                } else {
                    inputs.put(input, a);
                    inputsC.put(input, relCostA);
                }
            }
        }
        for (Long input : inputs.keySet()) {
            Link b = gp.addDirectedEdge(input, contractNode);
            arcContractionLink.put(b, inputs.get(input));
            virtCost.put(b, inputsC.get(input));
        }
    }
    private void setContractionOutputs(Long contractNode, HashSet<Long> cycle) {
        HashMap<Long, Link> outputs = new HashMap<Long, Link>();
        HashMap<Long, Integer> outputsC = new HashMap<Long, Integer>();

        Link a;
        Iterator<Link> it;
        for (Long n : cycle) {

            it = gp.getOutputArcsIterator(n);
            while (it.hasNext()) {
                a = it.next();
                Long output = a.getDst().getLong();
                if (cycle.contains(output))
                    continue;

                int costA = getCost(a);
                Link b = outputs.get(output);
                if (b != null) {
                    int costB = outputsC.get(output);
                    if (costA < costB) {
                        outputs.put(output, a);
                        outputsC.put(output, costA);
                    }
                } else {
                    outputs.put(output, a);
                    outputsC.put(output, costA);
                }

            }

        }
        for (Long output : outputs.keySet()) {
            Link b = gp.addDirectedEdge(contractNode, output);
            arcContractionLink.put(b, outputs.get(output));
            virtCost.put(b, outputsC.get(output));
        }
    }
    private void returnCyclesBack(HashSet<Link> h) {
        for (Couple<Long, HashSet<Long>> contraction : contractions) {
            Long contractNode = contraction.first;
            HashSet<Long> cycle = contraction.second;

            Iterator<Link> it = gp.getInputArcsIterator(
                    contractNode);
            while (it.hasNext()) {
                Link b = it.next();
                if (!h.contains(b))
                    continue;
                Link a = arcContractionLink.remove(b);
                if (a != null)
                    arcOfNode.put(a.getDst().getLong(), a);
                h.remove(b);
                break;
            }

            it = gp.getOutputArcsIterator(contractNode);
            while (it.hasNext()) {
                Link c = it.next();
                if (h.remove(c))
                    h.add(arcContractionLink.get(c));
            }

            gp.removeVertice(contractNode);

            for (Long n : cycle) {
                if (gp.vrmVertices.contains(DatapathId.of(n))) {
                    gp.vrmVertices.remove(DatapathId.of(n));
                }
                h.add(arcOfNode.get(n));
            }
        }
    }

    private Comparator<Link> comp = new Comparator<Link>() {

        @Override
        public int compare(Link o1, Link o2) {
            Integer i1 = getCost(o1);
            Integer i2 = getCost(o2);
            if (i1 == null)
                return -1;
            else if (i1.equals(i2))
                return 0;
            else if (i2 == null)
                return 1;
            else
                return i1.compareTo(i2);
        }

    };
    private Integer getCost(Link a) {
        Integer i = virtCost.get(a);
        if (i == null)
            i = getLinkCost(this.linkCost,a);// instance.getIntCost(a);
        return i;
    }

    private int getLinkCost(Map<Link, Integer> linkCost, Link theLinkNormal) {
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
