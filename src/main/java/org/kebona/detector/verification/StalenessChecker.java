package org.kebona.detector.verification;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kebona.detector.model.OntologyRecord;
import org.kebona.detector.model.ReuseRelationship;
import org.kebona.detector.model.VerificationResult.StalenessVerdict;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

/**
 * Compares a reused ontology's pinned version date against its current
 * latest version date -- NOT the portal's last-crawled date. A second,
 * deliberately live, non-reproducible dependency (alongside LinkChecker),
 * since "current latest" cannot be known from anything sitting locally.
 */
public class StalenessChecker {

    // matches a bare YYYY-MM-DD anywhere in a versionIRI or versionInfo string
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public StalenessVerdict check(ReuseRelationship relationship, OntologyRecord reusedRecord) {

        if (!relationship.isVersionPinned()) {
            // unpinned import always tracks "whatever's current" -- the
            // question isn't meaningful, regardless of anything else
            return StalenessVerdict.NOT_APPLICABLE;
        }

        if (reusedRecord == null || reusedRecord.getOntologyIri() == null) {
            // pinned, but the reused ontology isn't in our corpus, or was
            // never successfully loaded -- meaningful question, no data to answer it
            return StalenessVerdict.UNKNOWN;
        }

        Optional<LocalDate> pinnedDate = extractDate(reusedRecord.getVersionIri(), reusedRecord.getVersionInfo());
        if (pinnedDate.isEmpty()) {
            // pinned, but we can't tell what date the pinned version actually is
            return StalenessVerdict.UNKNOWN;
        }

        Optional<LocalDate> latestDate = fetchLatestDate(reusedRecord.getOntologyIri());
        if (latestDate.isEmpty()) {
            // pinned, dated, but the live lookup failed or was unreachable --
            // same "don't know" reasoning as LinkChecker's NOT_CHECKED
            return StalenessVerdict.UNKNOWN;
        }

        return pinnedDate.get().isBefore(latestDate.get()) ? StalenessVerdict.STALE : StalenessVerdict.CURRENT;
    }

    private Optional<LocalDate> extractDate(String versionIri, String versionInfo) {
        Optional<LocalDate> fromIri = tryExtract(versionIri);
        if (fromIri.isPresent()) {
            return fromIri;
        }
        return tryExtract(versionInfo);
    }

    private Optional<LocalDate> tryExtract(String source) {
        if (source == null) {
            return Optional.empty();
        }
        Matcher matcher = DATE_PATTERN.matcher(source);
        if (matcher.find()) {
            try {
                return Optional.of(LocalDate.parse(matcher.group(1), DATE_FORMAT));
            } catch (Exception e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private Optional<LocalDate> fetchLatestDate(String bareOntologyIri) {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        manager.setOntologyLoaderConfiguration(
                manager.getOntologyLoaderConfiguration()
                        .setConnectionTimeout(10000) // milliseconds
        );
        manager.getOntologyConfigurator()
                .setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);

        try {
            OWLOntology latest = manager.loadOntology(IRI.create(bareOntologyIri));

            String latestVersionIri = latest.getOntologyID().getVersionIRI()
                    .map(IRI::toString).orElse(null);

            String latestVersionInfo = latest.annotations()
                    .filter(a -> a.getProperty().getIRI().equals(OWLRDFVocabulary.OWL_VERSION_INFO.getIRI()))
                    .findFirst()
                    .flatMap(a -> a.getValue().asLiteral())
                    .map(lit -> lit.getLiteral())
                    .orElse(null);

            return extractDate(latestVersionIri, latestVersionInfo);

        } catch (OWLOntologyCreationException e) {
            return Optional.empty();
        }
    }
}