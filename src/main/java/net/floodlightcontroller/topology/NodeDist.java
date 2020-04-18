package net.floodlightcontroller.topology;

import org.projectfloodlight.openflow.types.DatapathId;

public class NodeDist implements Comparable<NodeDist> {
    private final DatapathId node;
    public DatapathId getNode() {
        return node;
    }

    private final int dist;
    public int getDist() {
        return dist;
    }

    public NodeDist(DatapathId node, int dist) {
        this.node = node;
        this.dist = dist;
    }

    @Override
    public int compareTo(NodeDist o) {
        if (o.dist == this.dist) {
            return (int)(this.node.getLong() - o.node.getLong());
        }
        return this.dist - o.dist;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NodeDist other = (NodeDist) obj;
        if (node == null) {
            if (other.node != null)
                return false;
        } else if (!node.equals(other.node))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        assert false : "hashCode not designed";
        return 42;
    }
}
