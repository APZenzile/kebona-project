package org.kebona.detector.detection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.kebona.detector.model.OntologyRecord;
import org.kebona.detector.model.ReuseMechanism;
import org.kebona.detector.model.ReuseRelationship;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * First-cut interpretation (not previously designed -- see handoff).
 * Flags reuse via equivalentClass axioms that assert one of THIS
 * ontology's own classes is equivalent to a class in a different
 * ontology's namespace -- a semantic-alignment reuse pattern distinct
 * from a formal owl:imports.
 *
 * Deliberate scope limit: only pairwise equivalences where exactly one
 * side is "this ontology's own" are reported. Equivalences between two
 * externally-defined classes are skipped -- that would describe a
 * relationship between two OTHER ontologies, not something `record`
 * itself is reusing.
 */
public class EquivalentClassDetector implements ReuseDetector {

    @Override
    public List<ReuseRelationship> detect(OWLOntology ontology, OntologyRecord record,
            List<OntologyRecord> registry) {
        List<ReuseRelationship> relationships = new ArrayList<>();
        String ownIri = record.getOntologyIri();

        ontology.axioms(AxiomType.EQUIVALENT_CLASSES).forEach(axiom -> {
            List<OWLClass> classes = axiom.getClassExpressions().stream()
                    .filter(ce -> !ce.isAnonymous())
                    .map(ce -> ce.asOWLClass())
                    .toList();

            if (classes.size() != 2) {
                return; // 3+ way equivalence -- corner case, not designed for yet
            }

            OWLClass first = classes.get(0);
            OWLClass second = classes.get(1);
            OWLClass ownClass = belongsToOntology(first, ownIri) ? first
                    : belongsToOntology(second, ownIri) ? second : null;
            OWLClass externalClass = ownClass == first ? second : ownClass == second ? first : null;

            if (ownClass == null || externalClass == null) {
                return;
            }

            String declaredIri = externalClass.getIRI().toString();
            Optional<OntologyRecord> matched = IriRegistryMatcher.match(declaredIri, registry);
            boolean inCorpus = matched.isPresent();
            String reusedAcronym = matched.map(OntologyRecord::getAcronym)
                    .orElseGet(() -> IriRegistryMatcher.guessAcronym(declaredIri));
            boolean pinned = IriRegistryMatcher.looksVersionPinned(declaredIri);

            relationships.add(new ReuseRelationship(
                    record.getAcronym(), reusedAcronym, inCorpus,
                    ReuseMechanism.EQUIVALENT_CLASS, declaredIri, pinned));
        });

        return relationships;
    }

    private boolean belongsToOntology(OWLClass owlClass, String ownIri) {
        if (ownIri == null) {
            return false;
        }
        return owlClass.getIRI().getNamespace().startsWith(stripFragment(ownIri));
    }

    private String stripFragment(String iri) {
        int hashIdx = iri.indexOf('#');
        return hashIdx >= 0 ? iri.substring(0, hashIdx) : iri;
    }
}