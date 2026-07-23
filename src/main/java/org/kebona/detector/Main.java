package org.kebona.detector;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.kebona.detector.detection.AlignmentDetector;
import org.kebona.detector.detection.AnnotationProvenanceDetector;
import org.kebona.detector.detection.EquivalentClassDetector;
import org.kebona.detector.detection.ImportsDetector;
import org.kebona.detector.detection.ReuseDetector;
import org.kebona.detector.graph.Graph;
import org.kebona.detector.graph.ReuseGraphBuilder;
import org.kebona.detector.loader.CatalogBuilder;
import org.kebona.detector.loader.OntologyLoader;
import org.kebona.detector.model.OntologyRecord;
import org.kebona.detector.model.PipelineContext;
import org.kebona.detector.model.ReuseRelationship;
import org.kebona.detector.model.VerificationResult;
import org.kebona.detector.output.CliReporter;
import org.kebona.detector.output.JsonResultWriter;
import org.kebona.detector.registry.RegistryLoader;
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
        this.detectors.add(new ImportsDetector());
        this.detectors.add(new AlignmentDetector());
        this.detectors.add(new AnnotationProvenanceDetector());
        this.detectors.add(new EquivalentClassDetector());

    }

    public static void main(String[] args) {
        new Main(args).run();
    }

    /** ------------------------------------------------------ */
    /** -------------------- the pipeline methods ------------ */
    /** ------------------------------------------------------ */

    public void run() {
        loadRegistry();
        loadAndDetect();
        buildGraph();
        verifyRelations();
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

        for (OntologyRecord record : registry) {
            OWLOntology ontology;
            try {
                ontology = loader.load(record);
            } catch (OWLOntologyCreationException e) {
                System.out.println("Failed to load " + record.getAcronym() + ": " + e.getMessage());
                continue;
            }

            for (ReuseDetector detector : this.detectors) {
                List<ReuseRelationship> relations = detector.detect(ontology, record, registry);
                pipelineContext.addRelationships(relations);
            }
        }
    }

    private void buildGraph() {
        Graph graph = new ReuseGraphBuilder().build(pipelineContext.getRelationships());
        pipelineContext.setReuseGraph(graph);
    }

    private void verifyRelations() {
        Map<String, OntologyRecord> registryByAcronym = pipelineContext.getRegistry().stream()
                .collect(Collectors.toMap(OntologyRecord::getAcronym, r -> r));

        for (ReuseRelationship relationship : pipelineContext.getRelationships()) {
            VerificationResult result = verifier.verify(relationship, registryByAcronym);
            pipelineContext.addVerification(result);
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
}