package org.kebona.detector.detection;

import java.util.List;

import org.kebona.detector.model.OntologyRecord;
import org.kebona.detector.model.ReuseRelationship;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Contract for a single reuse-detection mechanism (owl:imports,
 * equivalentClass, annotation provenance, alignment, ...). Main holds a
 * List<ReuseDetector> and calls detect() on each, uniformly, once per
 * loaded ontology -- this is why it's an interface: no shared
 * implementation between mechanisms, only a shared contract.
 */
public interface ReuseDetector {

    List<ReuseRelationship> detect(OWLOntology ontology, OntologyRecord record,
            List<OntologyRecord> registry);
}