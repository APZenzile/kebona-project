package org.kebona.detector.model;

/**
 * The outcome for one reuse relationship after staleness + link checking.
 * This is actually answers the research question: is it
 * still safe to use this reused ontology as imported?
 *
 * stalenessVerdict and linkStatus are independent, evidence-backed checks
 * (one from comparing version dates, one from a live HTTP call).
 * overallStatus is the single derived field combining both into one
 * glance-at-it verdict for the report/CLI.
 */
public class VerificationResult {

    /** The relationship/link being verified */
    private final ReuseRelationship relationship;

    /**
     * The reused ontology's own update info AT THE PINNED/IMPORTED VERSION -
     * null if the import is unpinned (not meaningfully comparable).
     */
    private String reusedVersionOwnDate;

    /** The reused ontology's own update info at its CURRENT latest release. */
    private String latestOwnDate;

    /** Human-readable gap, e.g. "14 months" or "3 releases". */
    private String stalenessGap;

    /** The state of the relation measured by staleness */
    private StalenessVerdict stalenessVerdict;
    /** The state of the relation measure by linkStatus (broken/alive) */
    private LinkStatus linkStatus;
    /** The overall state from both measurements. */
    private OverallStatus overallStatus;

    public VerificationResult(ReuseRelationship relationship) {
        this.relationship = relationship;
        this.stalenessVerdict = StalenessVerdict.NOT_APPLICABLE;
        this.linkStatus = LinkStatus.NOT_CHECKED;
    }

    /** --------------------------------------- */
    /** -------- Getters & Setters ------------ */
    /** --------------------------------------- */

    public ReuseRelationship getRelationship() {
        return relationship;
    }

    public String getReusedVersionOwnDate() {
        return reusedVersionOwnDate;
    }

    public void setReusedVersionOwnDate(String reusedVersionOwnDate) {
        this.reusedVersionOwnDate = reusedVersionOwnDate;
    }

    public String getLatestOwnDate() {
        return latestOwnDate;
    }

    public void setLatestOwnDate(String latestOwnDate) {
        this.latestOwnDate = latestOwnDate;
    }

    public String getStalenessGap() {
        return stalenessGap;
    }

    public void setStalenessGap(String stalenessGap) {
        this.stalenessGap = stalenessGap;
    }

    public StalenessVerdict getStalenessVerdict() {
        return stalenessVerdict;
    }

    public void setStalenessVerdict(StalenessVerdict stalenessVerdict) {
        this.stalenessVerdict = stalenessVerdict;
    }

    public LinkStatus getLinkStatus() {
        return linkStatus;
    }

    public void setLinkStatus(LinkStatus linkStatus) {
        this.linkStatus = linkStatus;
    }

    public OverallStatus getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(OverallStatus overallStatus) {
        this.overallStatus = overallStatus;
    }

    /** --------------------------------------- */
    /** -------- Supporting enums -------------- */
    /** --------------------------------------- */

    /**
     * Three values, not a boolean - NOT_APPLICABLE is a real, distinct
     * state (unpinned import), instead of "unknown due to missing
     * data."
     */
    public enum StalenessVerdict {
        STALE, CURRENT, NOT_APPLICABLE
    }

    public enum LinkStatus {
        OK, BROKEN, NOT_CHECKED
    }

    /**
     * The single combined verdict - what CliReporter actually prints per
     * row. Extendable later (e.g. a future DEPRECATED_TERM category) if
     * Phase 2 detectors or real data reveal a failure mode not covered here.
     */
    public enum OverallStatus {
        RELIABLE, STALE, BROKEN, STALE_AND_BROKEN, UNRESOLVABLE
    }
}