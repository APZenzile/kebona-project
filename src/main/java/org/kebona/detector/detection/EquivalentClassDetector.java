package org.kebona.detector.detection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.kebona.detector.model.OntologyRecord;
import org.kebona.detector.model.ReuseMechanism;
import org.kebona.detector.model.ReuseRelationship;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.parameters.Imports;

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
        Set<String> seenAcronyms = new HashSet<>();
        Set<String> seenIRIs = new HashSet<>();

        String ownIri = record.getOntologyIri();

        ontology.axioms(AxiomType.EQUIVALENT_CLASSES, Imports.EXCLUDED).forEach(axiom -> {
            List<OWLClass> classes = axiom.getClassExpressions().stream()
                    .flatMap(ce -> ce.getClassesInSignature().stream()).toList();

            if (ownIri == null) {
                return;
            }

            OWLClass ownClass = null;

            for (OWLClass clazz : classes) {
                if (belongsToOntology(clazz, ownIri)) {
                    ownClass = clazz;
                    break;
                }
            }

            // System.out.println("Own class is: " + ownClass.getIRI().toString());

            for (OWLClass externalClass : classes) {

                // System.out.println(externalClass == ownClass);

                // if (externalClass != ownClass) {
                // System.err.println("external class: " + externalClass.getIRI().toString());
                // }

                if (externalClass.equals(ownClass)) {
                    continue;
                }

                String declaredIri = externalClass.getIRI().toString();

                if (!seenIRIs.add(declaredIri)) {
                    return;
                }

                Optional<OntologyRecord> matched = IriRegistryMatcher.matchEntity(declaredIri, registry);
                boolean inCorpus = matched.isPresent();

                String reusedAcronym = matched.map(OntologyRecord::getAcronym)
                        .orElseGet(() -> IriRegistryMatcher.guessAcronymForEquivalence(declaredIri));
                String ownAcronym = record.getAcronym();

                if (reusedAcronym != null
                        && (reusedAcronym.equals(ownAcronym) || reusedAcronym.contains(ownAcronym.toUpperCase()))) {
                    return;
                }

                boolean pinned = IriRegistryMatcher.looksVersionPinned(declaredIri);

                if (!seenAcronyms.add(reusedAcronym)) {
                    return;
                }

                relationships.add(new ReuseRelationship(
                        record.getAcronym(),
                        reusedAcronym,
                        inCorpus,
                        ReuseMechanism.EQUIVALENT_CLASS,
                        declaredIri, pinned));
            }
        });

        return relationships;
    }

    private boolean belongsToOntology(OWLClass owlClass, String ownIri) {
        if (ownIri == null) {
            return false;
        }
        // System.out.println(owlClass.getIRI().getNamespace());
        // System.err.println(stripFragment(ownIri));
        // System.err.println("seperator");
        return owlClass.getIRI().getNamespace().startsWith(stripFragment(ownIri));
    }

    private String stripFragment(String iri) {
        int hashIdx = iri.indexOf('#');
        return hashIdx >= 0 ? iri.substring(0, hashIdx) : iri;
    }
}