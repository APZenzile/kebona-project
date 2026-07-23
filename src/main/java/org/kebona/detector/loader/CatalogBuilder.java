package org.kebona.detector.loader;

import java.io.File;
import java.nio.file.Path;

import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.util.AutoIRIMapper;

/**
 * Builds an offline IRI mapper so that owl:imports declarations resolve to
 * local files instead of the network. Wraps OWL API's own AutoIRIMapper,
 * which scans a local directory, reads each file's declared ontology IRI
 * (a lightweight header read, not a full parse), and indexes IRI -> file.
 *
 * Deliberately stateless: one factory method, no fields, no constructor
 * logic beyond the default.
 */
public class CatalogBuilder {

    public OWLOntologyIRIMapper buildMapper(Path ontologyDirectory) {
        File dir = ontologyDirectory.toFile();
        return new AutoIRIMapper(dir, true);
    }
}