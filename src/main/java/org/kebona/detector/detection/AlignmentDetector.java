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

/**
 * First-cut interpretation -- and genuinely the least settled of the four.
 * "Alignment" here means explicit SKOS mapping relations (exactMatch,
 * closeMatch, broadMatch, narrowMatch) between one of this ontology's own
 * classes and a class in another namespace.
 *
 * Open question worth deciding, not just an implementation gap: in the
 * ontology-matching literature, "alignment" more often means a SEPARATE
 * mapping file (OAEI/Alignment API format) distinct from either ontology,
 * not in-line SKOS annotations. Your corpus, as downloaded, doesn't
 * include any such external alignment files -- so this detector can only
 * find in-line SKOS signals, and will silently find nothing wherever
 * alignments (if any exist) live in files this pipeline never reads.
 */
public class AlignmentDetector implements ReuseDetector {

    /**
     * Set of universal identifiers for the skos properties used to describe entity
     * alignment
     */
    private static final Set<IRI> SKOS_MAPPING_PROPERTIES = Set.of(
            IRI.create("http://www.w3.org/2004/02/skos/core#exactMatch"),
            IRI.create("http://www.w3.org/2004/02/skos/core#closeMatch"),
            IRI.create("http://www.w3.org/2004/02/skos/core#broadMatch"),
            IRI.create("http://www.w3.org/2004/02/skos/core#narrowMatch"),
            IRI.create("http://www.w3.org/2004/02/skos/core#relatedMatch"));

    @Override
    public List<ReuseRelationship> detect(OWLOntology ontology, OntologyRecord record, List<OntologyRecord> registry) {
        /** Sets up a list of relationships to collect from detecting. */
        List<ReuseRelationship> relationships = new ArrayList<>();
        /** Get the universal identifier for the current ontology */
        String ownIri = record.getOntologyIri();

        ontology.axioms(AxiomType.ANNOTATION_ASSERTION).forEach(axiom -> {
            /** Narrow down the search space to only the skos alignment properties */
            if (!SKOS_MAPPING_PROPERTIES.contains(axiom.getProperty().getIRI())) {
                return;
            }

            String targetIri = axiom.getValue().asIRI().map(IRI::toString).orElse(null);
            if (targetIri == null) {
                return;
            }

            /** Might have to double checkl this during testing. */
            if (ownIri != null && targetIri.startsWith(stripFragment(ownIri))) {
                return;
            }

            Optional<OntologyRecord> matched = IriRegistryMatcher.matchEntity(targetIri, registry);
            boolean inCorpus = matched.isPresent();
            String reusedAcronym = matched.map(OntologyRecord::getAcronym)
                    .orElseGet(() -> IriRegistryMatcher.guessAcronym(targetIri));
            boolean pinned = IriRegistryMatcher.looksVersionPinned(targetIri);

            relationships.add(new ReuseRelationship(
                    record.getAcronym(),
                    reusedAcronym,
                    inCorpus,
                    ReuseMechanism.ALIGNMENT,
                    targetIri,
                    pinned));
        });

        return relationships;
    }

    private String stripFragment(String iri) {
        int hashIdx = iri.indexOf('#');
        return hashIdx >= 0 ? iri.substring(0, hashIdx) : iri;
    }
}