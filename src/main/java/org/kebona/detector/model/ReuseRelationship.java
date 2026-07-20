package org.kebona.detector.model;

/**
 * One detected reuse relationship - Produced by the detection
 * stage (one per file, per mechanism found, depth = 1), then extended by
 * the graph builder with depth 2+ relationships found by walking the
 * corpus-wide adjacency graph.
 *
 * NOTE (flagged, not yet fully resolved): declaredIri and versionPinned
 * were designed with owl:imports specifically in mind. Kept flat here since
 * ImportsDetector is the only detector being built right now - if a Phase 2
 * mechanism (equivalentClass, annotation-provenance, alignment) turns out
 * not to fit this shape, that's the point to reconsider, not before.
 */
public class ReuseRelationship {

    /** --------------------------------- */
    /** ---------- Object definition ---- */
    /** --------------------------------- */

    /** Acronym of the ontology reusing the other. */
    private final String importingOntology;

    /**
     * Acronym (or bare IRI, if outside the local corpus) of the ontology
     * being reused.
     */
    private final String reusedOntology;

    /**
     * True if reusedOntology is one of the downloaded files; false if the
     * importing ontology points somewhere outside the corpus
     */
    private final boolean reusedOntologyInCorpus;

    private final ReuseMechanism mechanism;

    /**
     * The exact IRI declared in the owl:imports (or equivalent) statement -
     * needed to tell a version-pinned import.
     */
    private final String declaredIri;

    /**
     * Whether declaredIri points at a specific dated release. Changes what
     * "staleness" even means for this edge - see design discussion.
     */
    private final boolean versionPinned;

    /**
     * 1 = direct reuse, found by a detector reading the file itself.
     * 2+ = found only by the graph builder walking the transitive chain.
     */
    private int depth;

    public ReuseRelationship(String importingOntology, String reusedOntology,
            boolean reusedOntologyInCorpus, ReuseMechanism mechanism,
            String declaredIri, boolean versionPinned) {
        this.importingOntology = importingOntology;
        this.reusedOntology = reusedOntology;
        this.reusedOntologyInCorpus = reusedOntologyInCorpus;
        this.mechanism = mechanism;
        this.declaredIri = declaredIri;
        this.versionPinned = versionPinned;
        this.depth = 1;
    }

    /** --------------------------------------- */
    /** -------- Getters & Setters ------------ */
    /** --------------------------------------- */

    public String getImportingOntology() {
        return importingOntology;
    }

    public String getReusedOntology() {
        return reusedOntology;
    }

    public boolean isReusedOntologyInCorpus() {
        return reusedOntologyInCorpus;
    }

    public ReuseMechanism getMechanism() {
        return mechanism;
    }

    public String getDeclaredIri() {
        return declaredIri;
    }

    public boolean isVersionPinned() {
        return versionPinned;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }
}
