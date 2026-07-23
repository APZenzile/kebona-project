package org.kebona.detector.output;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.kebona.detector.model.OntologyRecord;
import org.kebona.detector.model.ReuseRelationship;
import org.kebona.detector.model.VerificationResult;
import org.kebona.detector.model.VerificationResult.OverallStatus;

/**
 * Prints a readable summary of a pipeline run to the console. GUI explicitly
 * out of scope. Takes narrow inputs (not PipelineContext) per the rule that
 * no stage class ever receives the whole pipeline object.
 */
public class CliReporter {

    public void report(List<OntologyRecord> registry,
            List<ReuseRelationship> relationships,
            List<VerificationResult> verifications) {

        System.out.println("=== KeBoNa Ontology Reuse Detector: Summary ===");
        System.out.println("Ontologies in registry:          " + registry.size());
        System.out.println("Direct reuse relationships found: " + relationships.size());
        System.out.println("Verifications completed:          " + verifications.size());
        System.out.println();

        if (!verifications.isEmpty()) {
            Map<OverallStatus, Long> counts = verifications.stream()
                    .collect(Collectors.groupingBy(VerificationResult::getOverallStatus, Collectors.counting()));

            System.out.println("--- Overall status breakdown ---");
            for (OverallStatus status : OverallStatus.values()) {
                System.out.println(status + ": " + counts.getOrDefault(status, 0L));
            }
            System.out.println();
        }

        System.out.println("--- Direct relationships ---");
        System.out.printf("%-12s %-12s %-22s %-10s%n", "FROM", "TO", "MECHANISM", "IN CORPUS");
        for (ReuseRelationship r : relationships) {
            System.out.printf("%-12s %-12s %-22s %-10s%n",
                    r.getImportingOntology(), r.getReusedOntology(),
                    r.getMechanism(), r.isReusedOntologyInCorpus());
        }
    }
}