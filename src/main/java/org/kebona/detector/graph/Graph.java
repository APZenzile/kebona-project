package org.kebona.detector.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kebona.detector.model.ReuseRelationship;

/**
 * Adjacency structure over the corpus's direct reuse edges. Nodes are
 * ontology acronyms; edges are the real ReuseRelationship objects produced
 * by detection -- nothing synthesized. Transitive reuse is exposed purely
 * as a traversal (a path = an ordered list of real edges), not as new
 * ReuseRelationship objects.
 */
public class Graph {

    private final Map<String, List<ReuseRelationship>> adjacency = new HashMap<>();

    void addEdge(ReuseRelationship relationship) {
        adjacency.computeIfAbsent(relationship.getImportingOntology(), k -> new ArrayList<>())
                .add(relationship);
    }

    public List<ReuseRelationship> getDirectEdges(String acronym) {
        return adjacency.getOrDefault(acronym, Collections.emptyList());
    }

    /**
     * DFS from the given node. Returns every path reachable from it, as a
     * list of edge-chains -- one inner list per path, depth 1 upward.
     * A cycle guard (visited-per-branch) prevents infinite recursion if
     * the corpus ever has a reuse cycle.
     */
    public List<List<ReuseRelationship>> getPathsFrom(String acronym) {
        List<List<ReuseRelationship>> paths = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        visited.add(acronym);
        dfs(acronym, new ArrayList<>(), visited, paths);
        return paths;
    }

    private void dfs(String current, List<ReuseRelationship> pathSoFar,
            Set<String> visited, List<List<ReuseRelationship>> paths) {
        for (ReuseRelationship edge : adjacency.getOrDefault(current, Collections.emptyList())) {
            String next = edge.getReusedOntology();

            List<ReuseRelationship> extended = new ArrayList<>(pathSoFar);
            extended.add(edge);
            paths.add(extended);

            if (!visited.contains(next)) {
                visited.add(next);
                dfs(next, extended, visited, paths);
                visited.remove(next);
            }
        }
    }
}