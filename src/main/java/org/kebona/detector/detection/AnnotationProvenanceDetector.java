package org.kebona.detector.detection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.kebona.detector.model.OntologyRecord;
import org.kebona.detector.model.ReuseMechanism;
import org.kebona.detector.model.ReuseRelationship;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

/**
 * First-cut interpretation (not previously designed). Flags reuse
 * signalled by provenance-style annotations pointing at another
 * ontology's IRI -- WITHOUT a formal owl:imports backing it. A
 * deliberately "softer" signal than ImportsDetector (machine-enforced)
 * or EquivalentClassDetector (formal, logical).
 *
 * Only ANNOTATION_PROPERTIES below are scanned, an intentionally small
 * named set -- most annotations (rdfs:label, rdfs:comment) carry no
 * provenance meaning and would just add noise.
 */
public class AnnotationProvenanceDetector implements ReuseDetector {

    private static final Set<IRI> ANNOTATION_PROPERTIES = Set.of(
            OWLRDFVocabulary.RDFS_IS_DEFINED_BY.getIRI(),
            IRI.create("http://purl.org/dc/terms/source"),
            IRI.create("http://www.geneontology.org/formats/oboInOwl#hasDbXref"));

    @Override
    public List<ReuseRelationship> detect(OWLOntology ontology, OntologyRecord record,
            List<OntologyRecord> registry) {
        List<ReuseRelationship> relationships = new ArrayList<>();
        String ownIri = record.getOntologyIri();

        ontology.axioms(AxiomType.ANNOTATION_ASSERTION).forEach(assertion -> {
            IRI propertyIri = assertion.getProperty().getIRI();
            if (!ANNOTATION_PROPERTIES.contains(propertyIri)) {
                return;
            }

            String value = assertion.getValue().asIRI().map(IRI::toString).orElse(null);
            if (value == null) {
                return; // a literal, not an IRI -- nothing to match
            }
            if (ownIri != null && value.startsWith(stripFragment(ownIri))) {
                return; // points at itself -- not reuse of another ontology
            }

            Optional<OntologyRecord> matched = IriRegistryMatcher.match(value, registry);
            boolean inCorpus = matched.isPresent();
            String reusedAcronym = matched.map(OntologyRecord::getAcronym)
                    .orElseGet(() -> IriRegistryMatcher.guessAcronym(value));
            boolean pinned = IriRegistryMatcher.looksVersionPinned(value);

            relationships.add(new ReuseRelationship(
                    record.getAcronym(), reusedAcronym, inCorpus,
                    ReuseMechanism.ANNOTATION_PROVENANCE, value, pinned));
        });

        return relationships;
    }

    private String stripFragment(String iri) {
        int hashIdx = iri.indexOf('#');
        return hashIdx >= 0 ? iri.substring(0, hashIdx) : iri;
    }
}