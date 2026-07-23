package org.kebona.detector;

import java.util.ArrayList;
import java.util.List;

import org.kebona.detector.model.*;
import org.kebona.detector.registry.*;
import org.kebona.detector.graph.*;
import org.kebona.detector.detection.*;
import org.kebona.detector.verification.*;
import org.kebona.detector.output.CliReporter;
import org.kebona.detector.loader.*;

public class Main {

    private final String registryPath;
    private final RegistryLoader registryLoader;
    private List<ReuseDetector> detectors = new ArrayList();
    private PipelineContext pipelineContext;
    private ReuseGraphBuilder graphBuilder;
    private CliReporter cliReporter;
    private Verifier verifier;

    public Main(String registryPath) {
        /** constructrs the fields above that do not */
        this.registryPath = registryPath;
        this.pipelineContext = new PipelineContext();
        this.graphBuilder = new GraphBuilder();
        this.registryLoader = new RegistryLoader();
    }

    public static void main(String[] args) {
        new Main(args[0]).run();
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
        /** load the ontology objects here and run the detecting part */
    }

    private void buildGraph() {
        /** from the ontologyrecord we build the graph. */
    }

    private void verifyRelations() {

    }

    private void report() {

    }
}