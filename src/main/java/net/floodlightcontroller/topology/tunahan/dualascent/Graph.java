package net.floodlightcontroller.topology.tunahan.dualascent;

import net.floodlightcontroller.linkdiscovery.Link;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U64;

import java.util.*;

public class Graph {

    public Map<DatapathId, Set<Link>> links = new HashMap<DatapathId, Set<Link>>();
    public Map<Link, Integer> linkCost = new HashMap<Link,Integer>();

    public Set<DatapathId> vrmVertices = new HashSet<DatapathId>();
    public Set<Link> vrmEdges = new HashSet<Link>();

    public Graph(Map<DatapathId, Set<Link>> links, boolean fillLinks) {
        if(fillLinks)
        {
            this.links = links;
        }else
        {
            links.forEach((dID , setLink) -> {
                this.links.put(dID, new HashSet<Link>());
//                System.out.println(dID+ "--"+ setLink!= null ? setLink.size() : "setlink was here");
            });
        }
    }

    public Graph() {
    }


    public boolean addVertice(Long node, Set<Link> original) {
        if (links.containsKey(DatapathId.of(node.longValue())))
            return false;
       if(original == null)
       {
           original= new HashSet<Link>();
       }
        links.put(DatapathId.of(node), original);
        return true;

    }

    public void removeVertice(Long vertice) {
        if (links.containsKey(DatapathId.of(vertice.longValue()))) {
            links.remove(DatapathId.of(vertice.longValue()));
        }
/*        for(DatapathId a : links.keySet())
        {
            for(Link b : links.get(a))
            {
                if(b.getDst().getLong() == vertice)
                {
                    links.get(a).remove(b);
                }
            }
        }*/


    }

    public ArrayList<Link> getInputArcs(Long n) {
        ArrayList<Link> col = new ArrayList<Link>();
        if (fillWithInputArcs(n, col))
            return col;
        else
            return null;
    }

    private boolean fillWithInputArcs(Long n, Collection<Link> col) {
        Set<Link> l = getInputs(n);
        if (l == null)
            return false;
        for (Link a : l)
            if (!this.hasVirtuallyRemoved(a))
                col.add(a);
        return true;
    }

    public boolean hasVirtuallyRemoved(Link a) {
        return a != null
                && (vrmVertices.contains(a.getSrc()) || vrmVertices.contains(a.getDst()));
    }

    private Set<Link> getLinks(Long n) {
        Set<Link> nodeLinks = links.get(DatapathId.of(n));
        return nodeLinks;
    }

     public Graph getInducedGraphFromArc(Collection<Link> arcs, Map<DatapathId, Set<Link>> links)
     {
         Map<DatapathId, Set<Link>> linksOFNewGraph = new HashMap<DatapathId, Set<Link>>();
         arcs.stream().forEach(linkOfH ->
         {
             DatapathId dI_H = linkOfH.getSrc();
             DatapathId dI_H_Dst = linkOfH.getDst();
             if(linksOFNewGraph.get(dI_H_Dst) == null)
             {
                 linksOFNewGraph.put(dI_H_Dst, new HashSet<Link>());
             }
             if(linksOFNewGraph.get(dI_H) == null)
             {
                 linksOFNewGraph.put(dI_H, new HashSet<Link>());
             }
             linksOFNewGraph.get(dI_H).add(linkOfH);
             /*Link linkViceVersa = getLinkByGivenValuesInsideLinks(linkOfH.getDst(), linkOfH.getSrc());//tersi
             if(linkViceVersa != null){
                 linksOFNewGraph.get(dI_H).add(linkViceVersa);
             }*/
         });
         Graph g = new Graph(linksOFNewGraph, true);
         return g;
     }

    public Link addDirectedEdge(Long input, Long output) {
        return addArc(input, output);//true degisken kaldirildi isDirected
    }

    public Link addArc(Long n1, Long n2) {
        if (n1 == null || n2 == null || !this.contains(n1) || !this.contains(n2))
            return null;
        Link a = new Link();
        a.setSrc(DatapathId.of(n1));
        a.setSrcPort(OFPort.of(1));
        a.setDst(DatapathId.of(n2));
        a.setDstPort(OFPort.of(2));
        a.setLatency(U64.of(50L));
        getOutputs(n1).add(a);
        getInputs(n1).add(a);
        if(! this.links.containsKey(DatapathId.of(n1))) this.links.put(DatapathId.of(n1) , new HashSet<Link>());
        this.links.get(DatapathId.of(n1)).add(a);
        return a;
    }

    public boolean contains(Long n) {
        return links.containsKey(DatapathId.of(n));
    }

/*    private Set<Link> getOutputs(Long n) {//verilen n nolu noddan çıkanlar aq
        Set<Link> nodeLinks = getLinks(n);
        if (nodeLinks == null)
            return null;
        return nodeLinks.stream().filter(link -> link.getSrc().getLong() == n).collect(Collectors.toSet());
    }*/

    private Set<Link> getOutputs(Long n) {//verilen n nolu noddan çıkanlar aq
        Set<Link>  returnVal = new HashSet<Link>();
        this.links.forEach((id,setLinks) -> {
            setLinks.forEach(linkItem ->{
                if(linkItem.getDst().getLong() == n.longValue())
                {
                    returnVal.add(linkItem);
                }
            });
        });
        return returnVal;
    }

    private Set<Link> getOutputsWithNoVirtuallyRemoved(Long n) {//verilen n nolu noddan çıkanlar aq
        Set<Link>  returnVal = new HashSet<Link>();
        this.links.forEach((id,setLinks) -> {
            setLinks.forEach(linkItem ->{
                if(linkItem.getSrc().getLong() == n.longValue())
                {
                    if(this.vrmVertices.contains(linkItem.getSrc()) || this.vrmVertices.contains(linkItem.getDst()))
                    {
                    } else {
                        returnVal.add(linkItem);
                    }
                }
            });
        });
        return returnVal;
    }

    private Set<Link> getInputsWithNoVirtuallyRemoved(Long n) {//n nolu noda girilenler ak
        Set<Link>  returnVal = new HashSet<Link>();
        this.links.forEach((id,setLinks) -> {
            setLinks.forEach(linkItem ->{
                if(linkItem.getDst().getLong() == n.longValue())
                {
                    if(this.vrmVertices.contains(linkItem.getSrc()) || this.vrmVertices.contains(linkItem.getDst()))
                    {
                    } else {
                        returnVal.add(linkItem);
                    }
                }
            });
        });
        return returnVal;
    }

    private Set<Link> getInputs(Long n) {//n nolu noda girilenler ak
        Set<Link>  returnVal = new HashSet<Link>();
        this.links.forEach((id,setLinks) -> {
            setLinks.forEach(linkItem ->{
                if(linkItem.getDst().getLong() == n.longValue())
                {
                    returnVal.add(linkItem);
                }
            });
        });
        return returnVal;
    }

    public ArrayList<Long> getOneDirectedCycle() {
        ArrayList<ArrayList<Long>> ar = this.getConnectedComponents();
        for (ArrayList<Long> comp : ar) {
            for (Long v : comp) {
                ArrayList<Long> cycle = getDirectedCycleFrom(v);
                if (cycle == null)
                    continue;
                else
                    return cycle;
            }
        }
        return null;
    }

    /**
     * @return The lists of connected components of this.
     */
    public ArrayList<ArrayList<Long>> getConnectedComponents() {
        HashSet<Long> read = new HashSet<Long>();
        ArrayList<ArrayList<Long>> l = new ArrayList<ArrayList<Long>>();
        ArrayList<Long> h;
        for (DatapathId n : links.keySet()) {
            if (read.contains(n.getLong()))
                continue;
            h = getConnectedComponent(n.getLong());
            read.addAll(h);
            l.add(h);
        }
        return l;
    }

    /**
     * @param n
     * @return The connected component of this containing the node n, null if n
     *         do not belong to this
     */
    public ArrayList<Long> getConnectedComponent(Long n) {
        LinkedList<Long> l = new LinkedList<Long>();
        HashSet<Long> h = new HashSet<Long>();
        l.add(n);
        Long u = l.pop();
        h.add(u);
        Iterator<Link> it = this.getAllNeighbourIterator(u);
        if (it == null)
            return null;

        do {
            while (it.hasNext())
                l.add(this.getNeighbourNode(u, it.next()));

            if (l.isEmpty())
                break;

            u = l.pop();
            if ((h.contains(u)))
                continue;
            h.add(u);
            it = this.getAllNeighbourIterator(u);

        } while (true);

        return new ArrayList<Long>(h);
    }

    public Iterator<Link> getAllNeighbourIterator(Long n) {

        final Iterator<Link> li = getInputArcsIterator(n);
        final Iterator<Link> lo = getOutputArcsIterator(n);
//        final Iterator<Link> lu = getUndirectedNeighbourEdgesIterator(n);

        if (li == null || lo == null)
            return null;

        return new Iterator<Link>() {

            @Override
            public boolean hasNext() {
                return lo.hasNext() || li.hasNext();
            }

            @Override
            public Link next() {
                if (li.hasNext())
                    return li.next();
                if (lo.hasNext())
                    return lo.next();
                return null;
            }

            @Override
            public void remove() {
            }
        };
    }

    public Iterator<Link> getInputArcsIterator(Long n) {
        Set<Link> l = getInputsWithNoVirtuallyRemoved(n);

        if (l == null)
            return null;
        return l.iterator();
    }

    public Iterator<Link> getOutputArcsIterator(Long n) {
        Set<Link> l = getOutputsWithNoVirtuallyRemoved(n);
        if (l == null)
            return null;
        return l.iterator();
    }

    private Set<Link> getUndirectedNeighours(Long n) {
        /*Set<Link> nodeLinks = getLinks(n);
        if (nodeLinks == null)
            return null;*/
        return null;
    }

    public ArrayList<Long> getDirectedCycleFrom(Long v) {

        if (v == null)
            return null;

        final HashMap<Long, Long> depth = new HashMap<Long, Long>();
        depth.put(v, Long.valueOf(1));

        TreeSet<Long> l = new TreeSet<Long>(new Comparator<Long>() {

            @Override
            public int compare(Long v1, Long v2) {
                int i = depth.get(v1).compareTo(depth.get(v2));
                if (i != 0)
                    return i;
                else
                    return 1;
            }

        });
        l.add(v);

        Long u, w;
        Link a;

        while (!l.isEmpty()) {
            u = l.pollFirst();
            Iterator<Link> it = this.getOutputArcsIterator(u);
            if (it == null)
                return null;
            while (it.hasNext()) {
                a = it.next();
                w = a.getDst().getLong();
                if (w.equals(v)) {
                    ArrayList<Long> cycle = new ArrayList<Long>();
                    while (!u.equals(v)) {

                        cycle.add(u);
                        Iterator<Link> it2 = this.getInputArcsIterator(u);
                        while (it2.hasNext()) {
                            w = it2.next().getSrc().getLong();
                            if (depth.get(w) != null
                                    && (depth.get(w).equals(depth.get(u) - 1))) {
                                u = w;
                                break;
                            }
                        }
                    }
                    cycle.add(v);

                    return cycle;
                }
                if (!depth.containsKey(w)) {
                    depth.put(w, depth.get(u) + 1);
                    l.add(w);
                }

            }
        }

        return null;
    }

    /**
     * @return The neighbour of n in this linked with a or null if either n or a
     * do not belong to this or if n do not belong to a, or if a or n
     * are virtually removed.
     */
    public Long getNeighbourNode(Long n, Link a) {
        if (!this.contains(n))
            return null;
        if (n.equals(a.getSrc().getLong()))
            return a.getDst().getLong();
        else if (n.equals(a.getDst().getLong()))
            return a.getSrc().getLong();
        else
            return null;

    }

/*    public Map<Link, Integer> getLinkCost() {
        return linkCost;
    }

    public void setLinkCost(Map<Link, Integer> linkCost) {
        this.linkCost = linkCost;
    }*/

    public boolean virtuallyRemoveVertice(Long n) {
        if (n == null || !this.contains(n))
            return false;
        else
            return vrmVertices.add(DatapathId.of(n));
    }

    public Map<DatapathId, Set<Link>> getLinksWithNoRemoved() {
        Map<DatapathId, Set<Link>> mapWithNoVirtuallyRemoved = new HashMap<DatapathId, Set<Link>>(this.links);
        this.links.keySet().stream().forEach(dID -> {
            if (this.vrmVertices.contains(dID)) {
                mapWithNoVirtuallyRemoved.remove(dID);
            }
        });
        return mapWithNoVirtuallyRemoved;
    }

    public Link getLinkByGivenValuesInsideLinks(DatapathId src, DatapathId dst)
    {
        for(DatapathId dID : links.keySet())
        {
            for(Link aLink : links.get(dID))
            {
                if(aLink.getSrc() == src && aLink.getDst() == dst)
                {
                    return aLink;
                }
            }
        }
        return null;
    }
}
