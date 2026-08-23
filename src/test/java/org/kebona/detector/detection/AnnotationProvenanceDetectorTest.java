// package org.kebona.detector.detection;

// import java.util.List;

// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertFalse;
// import static org.junit.jupiter.api.Assertions.assertTrue;
// import org.junit.jupiter.api.Test;
// import org.kebona.detector.model.OntologyRecord;
// import org.kebona.detector.model.ReuseMechanism;
// import org.kebona.detector.model.ReuseRelationship;
// import org.semanticweb.owlapi.model.IRI;
// import org.semanticweb.owlapi.model.OWLOntology;
// import org.semanticweb.owlapi.model.OWLOntologyManager;
// import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

// class AnnotationProvenanceDetectorTest {

// private static final IRI IS_DEFINED_BY =
// OWLRDFVocabulary.RDFS_IS_DEFINED_BY.getIRI();
// private static final IRI DC_SOURCE =
// IRI.create("http://purl.org/dc/terms/source");
// private static final IRI HAS_DBXREF =
// IRI.create("http://www.geneontology.org/formats/oboInOwl#hasDbXref");
// private static final IRI RDFS_LABEL =
// IRI.create("http://www.w3.org/2000/01/rdf-schema#label");

// private final AnnotationProvenanceDetector detector = new
// AnnotationProvenanceDetector();
// private final OntologyRecord self = TestFixtures.record("FOODON",
// "http://purl.obolibrary.org/obo/foodon.owl");
// private final OntologyRecord bfo = TestFixtures.record("BFO",
// "http://purl.obolibrary.org/obo/bfo.owl");
// private final IRI subject =
// IRI.create("http://purl.obolibrary.org/obo/foodon.owl#Apple");

// @Test
// void detectsIsDefinedByPointingAtRegistryOntology() throws Exception {
// OWLOntologyManager mgr = TestFixtures.manager();
// OWLOntology ont = TestFixtures.emptyOntology(mgr, self.getOntologyIri());
// TestFixtures.addIriAnnotation(mgr, ont, subject, IS_DEFINED_BY,
// "http://purl.obolibrary.org/obo/bfo.owl");

// List<ReuseRelationship> found = detector.detect(ont, self, List.of(self,
// bfo));

// assertEquals(1, found.size());
// assertEquals(ReuseMechanism.ANNOTATION_PROVENANCE,
// found.get(0).getMechanism());
// assertEquals("BFO", found.get(0).getReusedOntology());
// assertTrue(found.get(0).isReusedOntologyInCorpus());
// }

// @Test
// void retainsUnresolvedDbXrefTarget() throws Exception {
// OWLOntologyManager mgr = TestFixtures.manager();
// OWLOntology ont = TestFixtures.emptyOntology(mgr, self.getOntologyIri());

// TestFixtures.addIriAnnotation(mgr, ont, subject, HAS_DBXREF,
// "http://langual.org/langual.asp?SEARCH_TOPIC=A0155");

// List<ReuseRelationship> found = detector.detect(ont, self, List.of(self,
// bfo));

// assertEquals(1, found.size());
// assertFalse(found.get(0).isReusedOntologyInCorpus());
// assertEquals("LANGUAL", found.get(0).getReusedOntology());
// assertEquals(ReuseMechanism.ANNOTATION_PROVENANCE,
// found.get(0).getMechanism());
// }

// @Test
// void acceptsDbXrefShapedLikeAnOboPurlEvenIfNotInRegistry() throws Exception {
// OWLOntologyManager mgr = TestFixtures.manager();
// OWLOntology ont = TestFixtures.emptyOntology(mgr, self.getOntologyIri());
// TestFixtures.addIriAnnotation(mgr, ont, subject, HAS_DBXREF,
// "http://purl.obolibrary.org/obo/chebi.owl");

// List<ReuseRelationship> found = detector.detect(ont, self, List.of(self,
// bfo));

// assertEquals(1, found.size());
// assertFalse(found.get(0).isReusedOntologyInCorpus());
// assertEquals("CHEBI", found.get(0).getReusedOntology());
// }

// @Test
// void ignoresLiteralValuedAnnotations() throws Exception {
// OWLOntologyManager mgr = TestFixtures.manager();
// OWLOntology ont = TestFixtures.emptyOntology(mgr, self.getOntologyIri());
// TestFixtures.addLiteralAnnotation(mgr, ont, subject, DC_SOURCE, "not an IRI,
// just text");

// assertTrue(detector.detect(ont, self, List.of(self, bfo)).isEmpty());
// }

// @Test
// void ignoresSelfReferencingAnnotation() throws Exception {
// OWLOntologyManager mgr = TestFixtures.manager();
// OWLOntology ont = TestFixtures.emptyOntology(mgr, self.getOntologyIri());
// TestFixtures.addIriAnnotation(mgr, ont, subject, IS_DEFINED_BY,
// self.getOntologyIri() + "#Apple");

// assertTrue(detector.detect(ont, self, List.of(self, bfo)).isEmpty());
// }

// @Test
// void ignoresAnnotationPropertiesOutsideTheWatchedSet() throws Exception {
// OWLOntologyManager mgr = TestFixtures.manager();
// OWLOntology ont = TestFixtures.emptyOntology(mgr, self.getOntologyIri());
// TestFixtures.addIriAnnotation(mgr, ont, subject, RDFS_LABEL,
// "http://purl.obolibrary.org/obo/bfo.owl");

// assertTrue(detector.detect(ont, self, List.of(self, bfo)).isEmpty());
// }
// }