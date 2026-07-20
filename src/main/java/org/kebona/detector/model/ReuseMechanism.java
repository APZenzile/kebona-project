package org.kebona.detector.model;

/**
 * The distinct ways one ontology can reuse another, Detectors are built in this
 * order:
 * Phase 1: IMPORTS (this task's primary ask, and the simplest/most
 * reliable - built and validated first)
 * Phase 2+: everything else, layered in only once IMPORTS is trusted.
 */
public enum ReuseMechanism {

    /** owl:imports - formal, declared import. */
    IMPORTS,

    /** owl:equivalentClass / owl:equivalentProperty cross-ontology axiom. */
    EQUIVALENT_CLASS,

    /** rdfs:isDefinedBy, dc:source, or similar MIREOT-style annotation only. */
    ANNOTATION_PROVENANCE,

    /** External mapping/alignment */
    ALIGNMENT
}