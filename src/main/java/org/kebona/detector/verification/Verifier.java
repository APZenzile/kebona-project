package org.kebona.detector.verification;

import java.util.Map;

import org.kebona.detector.model.OntologyRecord;
import org.kebona.detector.model.ReuseRelationship;
import org.kebona.detector.model.VerificationResult;
import org.kebona.detector.model.VerificationResult.LinkStatus;
import org.kebona.detector.model.VerificationResult.OverallStatus;
import org.kebona.detector.model.VerificationResult.StalenessVerdict;

/**
 * Combines StalenessChecker + LinkChecker into one verdict on the actual
 * research question: is this reuse relationship still reliable?
 *
 * Composition, not inheritance -- StalenessChecker and LinkChecker share no
 * interface, since their inputs/outputs differ and nothing ever calls them
 * interchangeably. Verifier always calls both, by name, in a fixed sequence.
 */
public class Verifier {

    private final StalenessChecker stalenessChecker;
    private final LinkChecker linkChecker;

    public Verifier(StalenessChecker stalenessChecker, LinkChecker linkChecker) {
        this.stalenessChecker = stalenessChecker;
        this.linkChecker = linkChecker;
    }

    public VerificationResult verify(ReuseRelationship relationship,
            Map<String, OntologyRecord> registryByAcronym) {

        VerificationResult result = new VerificationResult(relationship);

        OntologyRecord reusedRecord = registryByAcronym.get(relationship.getReusedOntology());

        StalenessVerdict stalenessVerdict = stalenessChecker.check(relationship, reusedRecord);
        result.setStalenessVerdict(stalenessVerdict);

        LinkStatus linkStatus = linkChecker.check(relationship.getDeclaredIri());
        result.setLinkStatus(linkStatus);

        result.setOverallStatus(deriveOverallStatus(stalenessVerdict, linkStatus));

        return result;
    }

    private OverallStatus deriveOverallStatus(StalenessVerdict staleness, LinkStatus link) {
        boolean isStale = staleness == StalenessVerdict.STALE;
        boolean isBroken = link == LinkStatus.BROKEN;

        if (isStale && isBroken)
            return OverallStatus.STALE_AND_BROKEN;
        if (isStale)
            return OverallStatus.STALE;
        if (isBroken)
            return OverallStatus.BROKEN;
        if (staleness == StalenessVerdict.UNKNOWN)
            return OverallStatus.UNRESOLVABLE;
        if (staleness == StalenessVerdict.NOT_APPLICABLE)
            return OverallStatus.RELIABLE;
        return OverallStatus.RELIABLE;
    }
}