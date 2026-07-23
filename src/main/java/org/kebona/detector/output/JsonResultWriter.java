package org.kebona.detector.output;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.kebona.detector.model.OntologyRecord;
import org.kebona.detector.model.ReuseRelationship;
import org.kebona.detector.model.VerificationResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Writes the final pipeline results to data/output/ as JSON. Takes narrow
 * inputs (not PipelineContext), same reasoning as CliReporter.
 */
public class JsonResultWriter {

    public void write(Path outputDirectory,
            List<OntologyRecord> registry,
            List<ReuseRelationship> relationships,
            List<VerificationResult> verifications) throws IOException {

        File dir = outputDirectory.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        mapper.writeValue(new File(dir, "registry.json"), registry);
        mapper.writeValue(new File(dir, "relationships.json"), relationships);
        mapper.writeValue(new File(dir, "verifications.json"), verifications);
    }
}