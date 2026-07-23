package org.kebona.detector.detection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.kebona.detector.model.OntologyRecord;
import org.kebona.detector.model.ReuseMechanism;
import org.kebona.detector.model.ReuseRelationship;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Scans an ontology's direct owl:imports declarations. This is the
 * mechanism CatalogBuilder/AutoIRIMapper was built to support -- imports
 * were already resolved to local files during OntologyLoader.load(); this
 * detector just reads back which declarations existed.
 */
public class ImportsDetector implements ReuseDetector {

    @Override
    public List<ReuseRelationship> detect(OWLOntology ontology, OntologyRecord record,
            List<OntologyRecord> registry) {
        List<ReuseRelationship> relationships = new ArrayList<>();

        ontology.importsDeclarations().forEach(decl -> {
            String declaredIri = decl.getIRI().toString();

            Optional<OntologyRecord> matched = IriRegistryMatcher.match(declaredIri, registry);
            boolean inCorpus = matched.isPresent();
            String reusedAcronym = matched.map(OntologyRecord::getAcronym)
                    .orElseGet(() -> IriRegistryMatcher.guessAcronym(declaredIri));
            boolean pinned = IriRegistryMatcher.looksVersionPinned(declaredIri);

            relationships.add(new ReuseRelationship(
                    record.getAcronym(), reusedAcronym, inCorpus,
                    ReuseMechanism.IMPORTS, declaredIri, pinned));
        });

        return relationships;
    }
}