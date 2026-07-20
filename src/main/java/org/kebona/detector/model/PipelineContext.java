package org.kebona.detector.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The global pipeline data carry across all pipeline stages.
 *
 * Owned and mutated only by Main - no stage class ever receives a reference
 * to this object directly. Each stage takes narrow inputs and returns its
 * own output type; Main is the only place that assembles those outputs into
 * this object.
 *
 * All three lists are initialized empty, never null, so CliReporter can
 * safely render this object at any point in the pipeline - even before
 * every stage has run - without null checks.
 */
public class PipelineContext {

    /** list of ontology records from the registry loader */
    private List<OntologyRecord> registry = new ArrayList<>();
    /** list of re-use relations for each ontology detected */
    private List<ReuseRelationship> relationships = new ArrayList<>();
    /** list of verfication on each of the relations */
    private List<VerificationResult> verifications = new ArrayList<>();

    /** Empty constructor - it is only a data hub. */
    public PipelineContext() {
    }

    /** ---------------------------------------------------------------- */
    /** ---------------------- Getters & Setters ----------------------- */
    /** ---------------------------------------------------------------- */

    public List<OntologyRecord> getRegistry() {
        return registry;
    }

    public void setRegistry(List<OntologyRecord> registry) {
        this.registry = registry;
    }

    public List<ReuseRelationship> getRelationships() {
        return relationships;
    }

    /**
     * @brief add the relationships associated with each ontology
     * 
     * @param relations the relationships one ontology holds
     *                  with the ontologies it uses.
     * 
     */
    public void addRelationships(List<ReuseRelationship> relations) {
        this.relationships.addAll(relations);
    }

    /**
     * @brief resets the relationship after the graph has been build
     * 
     * @param relationships the re-built list of relationships by the graph.
     * 
     */
    public void setRelationships(List<ReuseRelationship> relationships) {
        this.relationships = relationships;
    }

    public List<VerificationResult> getVerifications() {
        return verifications;
    }

    /**
     * @brief adds a verification for each relation that
     *        exists between ontologies
     * 
     * @param verification the verfication object.
     */
    public void addVerification(VerificationResult verification) {
        this.verifications.add(verification);
    }

    /**
     * @brief Appends a batch at once, if verification is ever run/returned in bulk.
     * 
     * @param batchVerifications the bulk of verifications.
     */
    public void addVerifications(List<VerificationResult> batchVerifications) {
        this.verifications.addAll(batchVerifications);
    }
}