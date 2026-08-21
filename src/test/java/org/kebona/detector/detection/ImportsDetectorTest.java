package org.kebona.detector.detection;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.kebona.detector.model.OntologyRecord;
import org.kebona.detector.model.ReuseMechanism;
import org.kebona.detector.model.ReuseRelationship;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

class ImportsDetectorTest {

    private final ImportsDetector detector = new ImportsDetector();
    private final OntologyRecord self = TestFixtures.record("FOODON", "http://purl.obolibrary.org/obo/foodon.owl");
    private final OntologyRecord bfo = TestFixtures.record("BFO", "http://purl.obolibrary.org/obo/bfo.owl");

    @Test
    void detectsSingleImportAlreadyInCorpus() throws Exception {
        OWLOntologyManager mgr = TestFixtures.manager();
        OWLOntology ont = TestFixtures.emptyOntology(mgr, self.getOntologyIri());
        TestFixtures.addImport(mgr, ont, "http://purl.obolibrary.org/obo/bfo.owl");

        List<ReuseRelationship> found = detector.detect(ont, self, List.of(self, bfo));

        assertEquals(1, found.size());
        ReuseRelationship rel = found.get(0);
        assertEquals("FOODON", rel.getImportingOntology());
        assertEquals("BFO", rel.getReusedOntology());
        assertTrue(rel.isReusedOntologyInCorpus());
        assertEquals(ReuseMechanism.IMPORTS, rel.getMechanism());
        assertFalse(rel.isVersionPinned());
    }

    @Test
    void detectsImportNotInCorpusWithGuessedAcronym() throws Exception {
        OWLOntologyManager mgr = TestFixtures.manager();
        OWLOntology ont = TestFixtures.emptyOntology(mgr, self.getOntologyIri());
        TestFixtures.addImport(mgr, ont, "http://purl.obolibrary.org/obo/chebi.owl");

        List<ReuseRelationship> found = detector.detect(ont, self, List.of(self, bfo));

        assertEquals(1, found.size());
        assertFalse(found.get(0).isReusedOntologyInCorpus());
        assertEquals("CHEBI", found.get(0).getReusedOntology());
    }

    @Test
    void flagsDatedPurlImportAsVersionPinned() throws Exception {
        OWLOntologyManager mgr = TestFixtures.manager();
        OWLOntology ont = TestFixtures.emptyOntology(mgr, self.getOntologyIri());
        TestFixtures.addImport(mgr, ont, "http://purl.obolibrary.org/obo/bfo/2023-05-01/bfo.owl");

        List<ReuseRelationship> found = detector.detect(ont, self, List.of(self, bfo));

        assertTrue(found.get(0).isVersionPinned());
    }

    @Test
    void returnsEmptyListWhenNoImportsDeclared() throws Exception {
        OWLOntologyManager mgr = TestFixtures.manager();
        OWLOntology ont = TestFixtures.emptyOntology(mgr, self.getOntologyIri());

        assertTrue(detector.detect(ont, self, List.of(self, bfo)).isEmpty());
    }

    @Test
    void detectsEachOfMultipleDistinctImports() throws Exception {
        OWLOntologyManager mgr = TestFixtures.manager();
        OWLOntology ont = TestFixtures.emptyOntology(mgr, self.getOntologyIri());
        TestFixtures.addImport(mgr, ont, "http://purl.obolibrary.org/obo/bfo.owl");
        TestFixtures.addImport(mgr, ont, "http://purl.obolibrary.org/obo/chebi.owl");

        assertEquals(2, detector.detect(ont, self, List.of(self, bfo)).size());
    }
}
