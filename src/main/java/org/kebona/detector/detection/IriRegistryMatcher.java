package org.kebona.detector.detection;

import java.util.List;
import java.util.Optional;

import org.kebona.detector.model.OntologyRecord;

/**
 * Shared helper: match an IRI encountered during detection back to a known
 * OntologyRecord. Two-tier, because of a real ordering gap -- see detector
 * class comments for why registry.ontologyIri may still be null here.
 */
final class IriRegistryMatcher {

    private IriRegistryMatcher() {
    }

    static Optional<OntologyRecord> match(String candidateIri, List<OntologyRecord> registry) {
        if (candidateIri == null) {
            return Optional.empty();
        }

        for (OntologyRecord record : registry) {
            if (candidateIri.equals(record.getOntologyIri())) {
                return Optional.of(record);
            }
        }

        String lowerIri = candidateIri.toLowerCase();
        for (OntologyRecord record : registry) {
            String acronym = record.getAcronym();
            if (acronym != null && lowerIri.contains(acronym.toLowerCase())) {
                return Optional.of(record);
            }
        }

        return Optional.empty();
    }

    /**
     * best-effort guess so reusedOntology isn't left null even when the
     * reused thing isn't in our corpus at all -- a heuristic, not a fact.
     */
    static String guessAcronym(String iri) {
        if (iri == null) {
            return "UNKNOWN";
        }
        String cleaned = iri.replaceAll("/$", "");
        int lastSlash = cleaned.lastIndexOf('/');
        String tail = lastSlash >= 0 ? cleaned.substring(lastSlash + 1) : cleaned;
        return tail.replaceAll("\\.owl$", "").toUpperCase();
    }

    /**
     * heuristic: does this IRI look pinned to a dated/versioned release,
     * vs. a bare always-current identifier.
     */
    static boolean looksVersionPinned(String iri) {
        if (iri == null) {
            return false;
        }
        return iri.matches(".*/\\d{4}-\\d{2}-\\d{2}/.*")
                || iri.matches(".*/v\\d+(\\.\\d+)*/.*")
                || iri.matches(".*-\\d+\\.\\d+\\.owl$");
    }
}