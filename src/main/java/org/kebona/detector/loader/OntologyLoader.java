package org.kebona.detector.loader;

import java.io.File;
import java.util.Optional;

import org.kebona.detector.model.OntologyRecord;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

/**
 * Turns a raw local ontology file into a Java-processable OWLOntology,
 * using OWL API. As a side effect, populates OntologyRecord's
 * ontologyIri/versionIri/versionInfo fields, since registry.json alone
 * can't know these -- only actually parsing the file can.
 */
public class OntologyLoader {

    private final OWLOntologyManager manager;

    public OntologyLoader(OWLOntologyIRIMapper catalogMapper) {
        this.manager = OWLManager.createOWLOntologyManager();
        this.manager.getIRIMappers().add(catalogMapper);
        this.manager.setOntologyLoaderConfiguration(
                this.manager.getOntologyLoaderConfiguration()
                        .setConnectionTimeout(100000)
                        .setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT));
    }

    public OWLOntology load(OntologyRecord record) throws OWLOntologyCreationException {
        File file = new File(record.getFilePath());
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(file);

        OWLOntologyID id = ontology.getOntologyID();
        id.getOntologyIRI().ifPresent(iri -> record.setOntologyIri(iri.toString()));
        id.getVersionIRI().ifPresent(iri -> record.setVersionIri(iri.toString()));

        extractVersionInfo(ontology).ifPresent(record::setVersionInfo);

        return ontology;
    }

    private Optional<String> extractVersionInfo(OWLOntology ontology) {
        return ontology.annotations()
                .filter(a -> a.getProperty().getIRI().equals(
                        OWLRDFVocabulary.OWL_VERSION_INFO.getIRI()))
                .findFirst()
                .flatMap(a -> a.getValue().asLiteral())
                .map(lit -> lit.getLiteral());
    }
}