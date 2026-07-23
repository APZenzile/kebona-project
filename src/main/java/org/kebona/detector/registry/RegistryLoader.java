package org.kebona.detector.registry;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.kebona.detector.model.OntologyRecord;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RegistryLoader {

    /** The records for each ontology */
    private List<OntologyRecord> records = new ArrayList<>();

    public RegistryLoader() {
    }

    public List<OntologyRecord> load(Path registryPath) {

        ObjectMapper mapper = new ObjectMapper();
        File file = registryPath.toFile();

        /** convert json to java */
        Ontology[] ontologies = null;

        try {
            ontologies = mapper.readValue(file, Ontology[].class);
        } catch (Exception e) {
        }

        /** convert all ontologies into records */
        for (Ontology ontology : ontologies) {
            OntologyRecord record = new OntologyRecord(ontology.acronym, ontology.filePath,
                    ontology.sourceUrl, ontology.sha256, ontology.downloadDate);

            records.add(record);
        }

        return records;
    }

}
