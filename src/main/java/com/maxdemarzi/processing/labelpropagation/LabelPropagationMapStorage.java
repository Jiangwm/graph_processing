package com.maxdemarzi.processing.labelpropagation;

import com.maxdemarzi.processing.NodeCounter;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.neo4j.graphdb.*;

public class LabelPropagationMapStorage implements LabelPropagation {
    private final GraphDatabaseService db;
    private final long nodes;
    private Long2DoubleOpenHashMap labelMap;

    public LabelPropagationMapStorage(GraphDatabaseService db) {
        this.db = db;
        this.nodes = new NodeCounter().getNodeCount(db);
    }

    @Override
    public void compute(String label, String type, int iterations) {
        RelationshipType relationshipType = RelationshipType.withName(type);
        labelMap = new Long2DoubleOpenHashMap();
        boolean done = false;
        int iteration = 0;
        try ( Transaction tx = db.beginTx()) {
            ResourceIterator<Node> nodes = db.findNodes(DynamicLabel.label(label));
            while (nodes.hasNext()) {
                Node node = nodes.next();
                labelMap.put(node.getId(), node.getId());
            }

            while (!done) {
                done = true;
                iteration++;

                for( Relationship relationship : db.getAllRelationships()) {
                    if (relationship.isType(relationshipType)) {
                        long x = relationship.getStartNode().getId();
                        long y = relationship.getEndNode().getId();
                        if (x == y) { continue; }
                        if (labelMap.get(x) > labelMap.get(y)){
                            labelMap.put(x, labelMap.get(y));
                            done = false;
                        } else if (labelMap.get(x) < labelMap.get(y)) {
                            labelMap.put(y, labelMap.get(x));
                            done = false;
                        }
                    }
                }

                if (iteration > iterations) {
                    done = true;
                }
            }
        }
    }

    @Override
    public double getResult(long node) {
        return labelMap != null ? labelMap.getOrDefault(node, -1D) : -1;
    }

    @Override
    public long numberOfNodes() {
        return nodes;
    };

}