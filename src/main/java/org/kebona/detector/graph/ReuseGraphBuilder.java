package org.kebona.detector.graph;

import java.util.List;

import org.kebona.detector.model.ReuseRelationship;

// ReuseGraphBuilder - the global view of the network for reuse.

public class ReuseGraphBuilder {

    public Graph build(List<ReuseRelationship> relationships) {
        Graph graph = new Graph();
        for (ReuseRelationship relationship : relationships) {
            graph.addEdge(relationship);
        }
        return graph;
    }
}