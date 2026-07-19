# KeBoNa Ontology Reuse Detector

Detects ontology reuse (`owl:imports` and related mechanisms) across a corpus
of ontologies, builds the transitive reuse graph, and flags stale or broken
reused ontologies. Built for the NRF-funded KeBoNa project.

## Status: structure only, code deliberately empty

This is a project **skeleton** — directory structure and `pom.xml` only.
Every `.java` file exists (so the package layout is fixed) but its body has
been intentionally stripped back to just the `package` declaration. No class
signatures, no method contracts, no logic.

This is deliberate: the class/method-level design (what each class's public
contract looks like, what data flows between stages) is being worked out in
discussion first, before any of it is committed to code. See the project
conversation history for that design discussion as it develops.

## ⚠️ Important: not yet build-verified

This project was scaffolded in a sandboxed environment that **cannot reach
Maven Central** (`repo.maven.apache.org` returns 403 — blocked by network
egress rules, the same restriction that blocked direct ontology portal
downloads earlier in this project). That means `mvn compile` has **not**
been successfully run against this `pom.xml` from here.

**Before building on it further, run this on your own machine first:**

```bash
mvn compile
```

If dependency versions have drifted since this was written (`owlapi-distribution`
and `owlapi-api` were both confirmed current at `5.5.1` on Maven Central at
time of writing), or if anything else fails to resolve, that's expected to
possibly need a small version bump — not a sign the architecture is wrong.

## Project layout

```
kebona-detector/
├── pom.xml
├── data/
│   ├── registry.json       # machine-readable source of truth (from manifest.json)
│   ├── manifest.json        # original download provenance (source URL, sha256, date)
│   ├── ontologies/          # the 20 .owl/.ttl files downloaded so far
│   └── output/              # pipeline results land here (JSON records)
├── catalog/                  # generated offline IRI-mapping catalog (not yet built)
└── src/
    ├── main/java/org/kebona/detector/
    │   ├── Main.java                          # entry point (unwired)
    │   ├── model/                              # data classes - Records 1/2/3 from the tracking doc
    │   │   ├── OntologyRecord.java             # Record 1: registry entry
    │   │   ├── ReuseRelationship.java          # Record 2: one detected reuse edge
    │   │   ├── ReuseMechanism.java             # enum: IMPORTS, EQUIVALENT_CLASS, ANNOTATION_PROVENANCE, ALIGNMENT
    │   │   └── VerificationResult.java         # Record 3: staleness + link-check verdict, combined
    │   ├── registry/RegistryLoader.java        # reads registry.json
    │   ├── loader/
    │   │   ├── OntologyLoader.java             # OWL API wrapper, applies the offline IRI mapper
    │   │   └── CatalogBuilder.java             # builds the offline IRI mapper from registry.json
    │   ├── detection/
    │   │   ├── ReuseDetector.java              # strategy interface
    │   │   ├── ImportsDetector.java            # Phase 1 - build and validate this first
    │   │   ├── EquivalentClassDetector.java    # Phase 2 (stub)
    │   │   ├── AnnotationProvenanceDetector.java  # Phase 2 (stub)
    │   │   └── AlignmentDetector.java          # Phase 2+ (stub)
    │   ├── graph/ReuseGraphBuilder.java         # corpus-wide adjacency + transitive closure
    │   ├── verification/                        # ONE stage answering the research question:
    │   │   ├── StalenessChecker.java           #   is this reused ontology still reliable?
    │   │   └── LinkChecker.java                #   staleness (version dates) + link health,
    │   │                                        #   two checks feeding one verdict
    │   └── output/
    │       ├── JsonResultWriter.java
    │       └── CliReporter.java
    └── test/java/org/kebona/detector/detection/
        └── ImportsDetectorTest.java             # known-answer test (disabled until ImportsDetector exists)
```

## Design decisions agreed so far (not yet encoded in code)

- **Offline IRI resolution**: ontology IRIs need to map to local files, so
  `owl:imports` resolves against our frozen snapshot rather than the live
  network — both for reproducibility and because this sandbox can't reach
  the portals anyway. Where this logic lives (`CatalogBuilder`/`OntologyLoader`)
  is fixed; the exact method contract is not yet designed.
- **Detection is per-file; graph building is corpus-wide.** Detectors never
  see the rest of the corpus — they report what one file declares. Graph
  building is a separate pass over *all* detection output, because transitive
  closure genuinely needs the full picture assembled first.
- **No graph library dependency planned.** The "graph" is expected to be a
  simple map plus BFS/DFS, not a dedicated library — open to revisiting.
- **Version-pinned vs unpinned imports need to be tracked separately**,
  because "staleness" means something different for each.
- **Staleness + link checking are one verification stage, one package**
  (`verification/`), not two separate ones — they're two checks feeding a
  single verdict on the actual research question: is this reused ontology
  still reliable to depend on? The model class carrying that verdict was
  renamed `VerificationResult` to match (previously `StalenessResult`).
- **Build order**: `ImportsDetector` first, validated against a manually
  checked known-answer test, before any other mechanism is added.
- **Report writing is out of scope.** This tool's job ends at JSON output +
  a CLI summary table; the report itself is written by hand from that data.

## Next steps

1. Finalize class/method-level design, stage by stage, in discussion.
2. Only once that's settled: write the actual implementation code.
3. Verify `mvn compile` succeeds on a machine with real network access.
4. Validate `ImportsDetector` against a manually-checked ontology before
   trusting it on the full corpus.
