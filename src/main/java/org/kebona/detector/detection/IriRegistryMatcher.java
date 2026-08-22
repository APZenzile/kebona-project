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

        /** First tries exact ontology/version IRI matches */
        for (OntologyRecord record : registry) {
            if (record.getOntologyIri() != null &&
                    candidateIri.equals(record.getOntologyIri())) {
                return Optional.of(record);
            }

            if (record.getVersionIri() != null &&
                    candidateIri.equals(record.getVersionIri())) {
                return Optional.of(record);
            }
        }

        /** No exact match. Identifies the ontology from the IRI. */
        String candidateAcronym = guessAcronym(candidateIri);

        /** Check whether that identified ontology exists in the registry. */
        for (OntologyRecord record : registry) {
            if (record.getAcronym() != null &&
                    record.getAcronym().equalsIgnoreCase(candidateAcronym)) {
                return Optional.of(record);
            }
        }

        return Optional.empty();
    }

    static Optional<OntologyRecord> matchEntity(String entityIri,
            List<OntologyRecord> registry) {

        if (entityIri == null) {
            return Optional.empty();
        }

        for (OntologyRecord record : registry) {

            String ontologyIri = record.getOntologyIri();
            if (ontologyIri != null &&
                    (entityIri.equals(ontologyIri)
                            || entityIri.startsWith(ontologyIri + "#")
                            || entityIri.startsWith(ontologyIri + "/"))) {
                return Optional.of(record);
            }

            String versionIri = record.getVersionIri();
            if (versionIri != null &&
                    (entityIri.equals(versionIri)
                            || entityIri.startsWith(versionIri + "#")
                            || entityIri.startsWith(versionIri + "/"))) {
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
        String tail = lastSlash >= 0
                ? cleaned.substring(lastSlash + 1)
                : cleaned;

        return tail.replaceAll("\\.owl$", "").toUpperCase();
    }

    static String guessAcronymForEquivalence(String iri) {
        if (iri == null) {
            return "UNKNOWN";
        }

        String cleaned = iri.replaceAll("/$", "");
        int lastSlash = cleaned.lastIndexOf('/');
        String tail = lastSlash >= 0
                ? cleaned.substring(lastSlash + 1)
                : cleaned;

        String acronym = tail.replaceAll("\\.owl$", "").toUpperCase();

        acronym = cleanAcronym(acronym);

        return acronym;
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

    static String cleanAcronym(String acro) {
        StringBuilder acronym = new StringBuilder();

        for (Character c : acro.toCharArray()) {
            if (Character.isLetter(c)) {
                acronym.append(c);
            }
        }

        return acronym.toString();
    }
}