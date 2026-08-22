package org.kebona.detector.detection;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.semanticweb.owlapi.model.parameters.Imports;
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

    // recognized ontology-hosting conventions
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
        Set<IRI> ownSignature = computeNativeSignature(record, ontology);
        Set<String> seenAcronyms = new HashSet<>();
        Set<String> seenIRIs = new HashSet<>();

        String ownIri = record.getOntologyIri();

        ontology.axioms(AxiomType.ANNOTATION_ASSERTION, Imports.EXCLUDED).forEach(assertion -> {

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

            String subjectIri = assertion.getSubject().isIRI()
                    ? assertion.getSubject().asIRI().map(IRI::toString).orElse(null)
                    : null;
            /**
             * Filters assertions whose subject isn't even part of the ontology under test.
             */
            if (ownIri != null && (subjectIri == null || !ownSignature.contains(IRI.create(subjectIri)))) {
                return;
            }

            /** Filters out self references */
            if (ownIri != null && value.startsWith(stripFragment(ownIri))) {
                return;
            }

            if (!seenIRIs.add(value)) {
                return;
            }

            Optional<OntologyRecord> matched = IriRegistryMatcher.match(value, registry);
            boolean inCorpus = matched.isPresent();

            /**
             * It is not in our collection of ontologies and Does not match the known
             * pattern
             */

            if (!inCorpus) {
                if (!resolvesToOntology(value)) {
                    /**
                     * try to resolve the IRI to get the actual resource to see whether is
                     * it actually an ontology or just an external web resource
                     */
                    return;
                }
            }

            /**
             * Sets the reused ontology by getting it from the matching ontology or guessing
             * it from IRI
             */
            String reusedAcronym = matched.map(OntologyRecord::getAcronym)
                    .orElseGet(() -> IriRegistryMatcher.guessAcronym(value));

            /** Looks whether the IRI has an associated version date */
            boolean pinned = IriRegistryMatcher.looksVersionPinned(value);

            if (!seenAcronyms.add(reusedAcronym)) {
                return;
            }

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

    private String stripFragment(String iri) {
        int hashIdx = iri.indexOf('#');
        return hashIdx >= 0 ? iri.substring(0, hashIdx) : iri;
    }

    private Set<IRI> computeNativeSignature(OntologyRecord record, OWLOntology ontology) {

        String filePath = record.getFilePath();

        if (filePath == null || filePath.isBlank() || !new File(filePath).isFile()) {
            /**
             * No real backing file to re-parse in isolation (e.g. in-memory test
             * fixtures) -- fall back to the ontology object's own signature.
             */
            return ontology.signature(Imports.EXCLUDED)
                    .map(entity -> entity.getIRI())
                    .collect(Collectors.toSet());
        }

        try {
            OWLOntologyManager isolatedManager = OWLManager.createOWLOntologyManager();

            /**
             * Force every import to fail to resolve, regardless of whether it's
             * locally cached or fetchable over the network -- guarantees nothing
             * from an import (headerless or not) can fuse into this parse.
             */
            isolatedManager.getIRIMappers()
                    .add(iri -> IRI.create("file:///dev/null/does-not-exist-" + iri.hashCode() + ".owl"));

            isolatedManager.setOntologyLoaderConfiguration(
                    isolatedManager.getOntologyLoaderConfiguration()
                            .setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT));

            OWLOntology isolated = isolatedManager
                    .loadOntologyFromOntologyDocument(new File(record.getFilePath()));

            return isolated.signature(Imports.EXCLUDED)
                    .map(entity -> entity.getIRI())
                    .collect(Collectors.toSet());

        } catch (OWLOntologyCreationException e) {
            return ontology.signature(Imports.EXCLUDED)
                    .map(entity -> entity.getIRI())
                    .collect(Collectors.toSet());
        }
    }

    private boolean resolvesToOntology(String iri) {
        try {
            /** Get the manager ready */
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            /** Create an IRIDocumentSource, assuming the IRI to be URL */
            OWLOntologyDocumentSource source = new IRIDocumentSource(IRI.create(iri));

            /** Set appropriate configurations */
            OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration()
                    .setConnectionTimeout(15_000)
                    .setFollowRedirects(true)
                    .setRetriesToAttempt(0)
                    .setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);

            // System.out.println("[DEBUG] processed IRI values: " + iri);

            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(source, config);

            return ontology.getOntologyID().getOntologyIRI().isPresent();

        } catch (OWLOntologyCreationException | RuntimeException e) {
            return true;
        }
    }
}
