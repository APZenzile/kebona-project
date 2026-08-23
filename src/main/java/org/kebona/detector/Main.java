package org.kebona.detector;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.kebona.detector.detection.AlignmentDetector;
import org.kebona.detector.detection.EquivalentClassDetector;
import org.kebona.detector.detection.ReuseDetector;
import org.kebona.detector.graph.Graph;
import org.kebona.detector.graph.ReuseGraphBuilder;
import org.kebona.detector.loader.CatalogBuilder;
import org.kebona.detector.loader.OntologyLoader;
import org.kebona.detector.model.OntologyRecord;
import org.kebona.detector.model.PipelineContext;
import org.kebona.detector.model.ReuseMechanism;
import org.kebona.detector.model.ReuseRelationship;
import org.kebona.detector.model.VerificationResult;
import org.kebona.detector.output.CliReporter;
import org.kebona.detector.output.JsonResultWriter;
import org.kebona.detector.registry.RegistryLoader;
import org.kebona.detector.verification.LinkChecker;
import org.kebona.detector.verification.StalenessChecker;
import org.kebona.detector.verification.Verifier;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class Main {

    private PipelineContext pipelineContext;
    private final Path registryPath;
    private final Path ontologyPath;
    private final Path outpuPath;

    private final RegistryLoader registryLoader;
    private final List<ReuseDetector> detectors = new ArrayList<>();

    private Verifier verifier;
    private CliReporter cliReporter;

    public Main(String[] args) {
        /** Initial stage: loading up ontology registry */
        this.registryPath = Paths.get(args[0]);
        this.ontologyPath = Paths.get(args[1]);
        this.outpuPath = Paths.get(args[2]);
        this.pipelineContext = new PipelineContext();

        /** Loading and detecting stages */
        this.registryLoader = new RegistryLoader();

        /** Adds all the detectors. */
        this.detectors.add(new AlignmentDetector());
        // this.detectors.add(new ImportsDetector());
        // this.detectors.add(new AnnotationProvenanceDetector());
        // this.detectors.add(new AlignmentDetector());

        this.verifier = new Verifier(new StalenessChecker(), new LinkChecker());
        this.cliReporter = new CliReporter();

    }

    public static void main(String[] args) {
        new Main(args).run();
    }

    /** ------------------------------------------------------ */
    /** -------------------- the pipeline methods ------------ */
    /** ------------------------------------------------------ */

    public void run() {
        System.out.println("[INFO] LOADING REGISTRY...");
        loadRegistry();
        System.out.println("[INFO] LOADING AND DETECTING...");
        loadAndDetect();
        // System.exit(0);

        System.out.println("[INFO] BUILDING GRAPH...");
        buildGraph();
        Map<ReuseMechanism, Long> byMechanism = pipelineContext.getRelationships().stream()
                .collect(Collectors.groupingBy(ReuseRelationship::getMechanism, Collectors.counting()));
        System.out.println(byMechanism);

        System.out.println("[INFO] VERIFYING RELATIONS...");
        verifyRelations();
        System.out.println("[INFO] REPORT RESULTS...");
        report();
    }

    /** ------------------------------------------------------------------- */
    /** ------------------- helper methods to runner ---------------------- */
    /** ------------------------------------------------------------------- */

    private void loadRegistry() {
        List<OntologyRecord> registry = registryLoader.load(registryPath);
        pipelineContext.setRegistry(registry);
    }

    private void loadAndDetect() {
        CatalogBuilder catalogMapper = new CatalogBuilder();
        OntologyLoader loader = new OntologyLoader(catalogMapper.buildMapper(ontologyPath));

        List<OntologyRecord> registry = pipelineContext.getRegistry();
        int i = 0;

        for (OntologyRecord record : registry) {
            OWLOntology ontology;

            System.out.println("\t[INFO] about to load the a record..." + record.getAcronym());

            try {
                ontology = loader.load(record);
                System.out.println("\t[INFO] successfully loaded record: " + ++i + " " + record.getAcronym());
            } catch (OWLOntologyCreationException e) {
                System.out.println("Failed to load " + record.getAcronym() + ": " +
                        e.getMessage());
                continue;
            }
            System.err.println("\t[INFO] Detecting relations");
            for (ReuseDetector detector : this.detectors) {
                List<ReuseRelationship> relations = detector.detect(ontology, record, registry);
                pipelineContext.addRelationships(relations);
            }
        }

        printRelations();
    }

    private void buildGraph() {
        Graph graph = new ReuseGraphBuilder().build(pipelineContext.getRelationships());
        pipelineContext.setReuseGraph(graph);
    }

    private void verifyRelations() {
        Map<String, OntologyRecord> registryByAcronym = pipelineContext.getRegistry().stream()
                .collect(Collectors.toMap(OntologyRecord::getAcronym, r -> r));
        // int i = 0;
        for (ReuseRelationship relationship : pipelineContext.getRelationships()) {
            // System.out.println("[INFO] about verify the relationship..");
            VerificationResult result = verifier.verify(relationship, registryByAcronym);
            // System.out.println("[INFO] relationship confirmed..." + ++i);
            pipelineContext.addVerification(result);
            // if (i == 100) {
            // break;
            // }
        }
    }

    private void report() {
        List<OntologyRecord> registry = pipelineContext.getRegistry();
        List<ReuseRelationship> relationships = pipelineContext.getRelationships();
        List<VerificationResult> verifications = pipelineContext.getVerifications();

        cliReporter.report(registry, relationships, verifications);

        try {
            new JsonResultWriter().write(this.outpuPath, registry, relationships, verifications);
        } catch (IOException e) {
            System.out.println("Failed to write JSON output: " + e.getMessage());
        }
    }

    private void printRelations() {

        System.out.println("Number of Relations: " + pipelineContext.getRelationships().size());

        for (ReuseRelationship relation : pipelineContext.getRelationships()) {
            System.out.println("==========================");
            System.out.println(relation.getImportingOntology() + " ----> " + relation.getReusedOntology());
            System.out.println("==========================");
        }
    }
}