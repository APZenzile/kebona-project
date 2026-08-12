package org.kebona.detector.detection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
// import java.util.regex.Pattern;

import org.kebona.detector.model.OntologyRecord;
import org.kebona.detector.model.ReuseMechanism;
import org.kebona.detector.model.ReuseRelationship;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
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
 *
 * ONTOLOGIES ONLY: many hasDbXref values are IRI-typed but point at
 * non-ontology databases (LanguaL, SIREN, ITIS taxon lookups, bare food
 * codes) rather than another ontology. A value is only accepted as reuse
 * if it (a) matches something in the local registry, or (b) structurally
 * looks like a real ontology IRI (OBO PURL / w3id convention). Anything
 * shaped like a database query string or lookup endpoint is rejected,
 * even if OWL API happily parsed it as an IRI.
 */
public class AnnotationProvenanceDetector implements ReuseDetector {

    private static final Set<IRI> ANNOTATION_PROPERTIES = Set.of(
            OWLRDFVocabulary.RDFS_IS_DEFINED_BY.getIRI(),
            IRI.create("http://purl.org/dc/terms/source"),
            IRI.create("http://www.geneontology.org/formats/oboInOwl#hasDbXref"));

    // // recognized ontology-hosting conventions
    // private static final Pattern ONTOLOGY_IRI_SHAPE = Pattern.compile(
    // "^https?://(purl\\.obolibrary\\.org/obo/|w3id\\.org/(emmo|.*ontology)).*",
    // Pattern.CASE_INSENSITIVE);

    // // clear signs of a database lookup / query endpoint, not an ontology
    // private static final Pattern NON_ONTOLOGY_SHAPE = Pattern.compile(
    // ".*(\\.asp\\?|\\?SEARCH_TOPIC=|\\?TERMID=|\\?RECORDID=|SIREN:|TAXON\\.PL\\?).*",
    // Pattern.CASE_INSENSITIVE);

    @Override
    public List<ReuseRelationship> detect(OWLOntology ontology, OntologyRecord record,
            List<OntologyRecord> registry) {
        List<ReuseRelationship> relationships = new ArrayList<>();
        String ownIri = record.getOntologyIri();

        ontology.axioms(AxiomType.ANNOTATION_ASSERTION).forEach(assertion -> {
            IRI propertyIri = assertion.getProperty().getIRI();

            /** The IRI is not one of those we want to assess */
            if (!ANNOTATION_PROPERTIES.contains(propertyIri)) {
                return;
            }

            String value = assertion.getValue().asIRI().map(IRI::toString).orElse(null);

            /** might note be a string literal or just not an IRI value */
            if (value == null) {
                return;
            }

            /** It references the same owl file. */
            if (ownIri != null && value.startsWith(stripFragment(ownIri))) {
                return;
            }

            Optional<OntologyRecord> matched = IriRegistryMatcher.match(value, registry);
            boolean inCorpus = matched.isPresent();

            /**
             * It is not in our collection of ontologies and Does not match the known
             * pattern
             */
            if (!inCorpus && !resolvesToOntology(value)) {
                /**
                 * try to resolve the IRI to get the actual resource to see whether is
                 * it actually an ontology or just an external web resource
                 */
                return;
            }

            /**
             * Sets the reused ontology by getting it from the matching ontology or guessing
             * it from IRI
             */
            String reusedAcronym = matched.map(OntologyRecord::getAcronym)
                    .orElseGet(() -> IriRegistryMatcher.guessAcronym(value));

            /** Looks whether the IRI has an associated version date */
            boolean pinned = IriRegistryMatcher.looksVersionPinned(value);

            relationships.add(new ReuseRelationship(
                    record.getAcronym(),
                    reusedAcronym,
                    inCorpus,
                    ReuseMechanism.ANNOTATION_PROVENANCE,
                    value,
                    pinned));
        });

        return relationships;
    }

    /******************************************************************************/
    /************************** HELPERS *******************************************/
    /******************************************************************************/

    // private boolean looksLikeOntology(String iri) {
    // if (NON_ONTOLOGY_SHAPE.matcher(iri).matches()) {
    // return false;
    // }
    // return ONTOLOGY_IRI_SHAPE.matcher(iri).matches();
    // }

    private String stripFragment(String iri) {
        int hashIdx = iri.indexOf('#');
        return hashIdx >= 0 ? iri.substring(0, hashIdx) : iri;
    }

    private boolean resolvesToOntology(String iri) {
        try {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

            OWLOntologyDocumentSource source = new IRIDocumentSource(IRI.create(iri));

            OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration()
                    .setMissingImportHandlingStrategy(
                            MissingImportHandlingStrategy.SILENT);

            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(source, config);

            return ontology.getOntologyID().getOntologyIRI().isPresent();

        } catch (OWLOntologyCreationException | RuntimeException e) {
            return false;
        }
    }
}