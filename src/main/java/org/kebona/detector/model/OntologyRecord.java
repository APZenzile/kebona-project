package org.kebona.detector.model;

/**
 * One entry in the master ontology registry.
 *
 * Populated in two passes:
 * 1. RegistryLoader constructs one of these per entry in registry.json,
 * with ontologyIri/versionIri/versionInfo left null - registry.json
 * doesn't know the true values, only OWL API parsing the actual file
 * can determine them.
 * 2. OntologyLoader, after loading the file via OWL API, writes the real
 * values back onto this SAME record via the setters below.
 *
 * The other five fields (acronym, file Path, sourceUrl, sha256, downloadDate)
 * are set once at construction and never change - hence constructor-only,
 * no setters for those.
 */
public class OntologyRecord {

    /** the ontology acronym */
    private final String acronym;
    /** the file path of the ontology */
    private final String filePath;
    /** the sourceURL from which it was downloaded. */
    private final String sourceUrl;
    /** the checksum of the ontology */
    private final String sha256;
    /** the date at which it was downloaded */
    private final String downloadDate;

    /** Unknown until OntologyLoader actually parses the file */
    private String ontologyIri;
    private String versionIri;
    private String versionInfo;

    public OntologyRecord(String acronym, String filePath, String sourceUrl,
            String sha256, String downloadDate) {
        this.acronym = acronym;
        this.filePath = filePath;
        this.sourceUrl = sourceUrl;
        this.sha256 = sha256;
        this.downloadDate = downloadDate;
    }

    /** --------------------------------------- */
    /** -------- Getters (construction-set) --- */
    /** --------------------------------------- */

    public String getAcronym() {
        return acronym;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getSha256() {
        return sha256;
    }

    public String getDownloadDate() {
        return downloadDate;
    }

    /** --------------------------------------------- */
    /** -------- Getters & Setters (parsed later) --- */
    /** --------------------------------------------- */

    public String getOntologyIri() {
        return ontologyIri;
    }

    public void setOntologyIri(String ontologyIri) {
        this.ontologyIri = ontologyIri;
    }

    public String getVersionIri() {
        return versionIri;
    }

    public void setVersionIri(String versionIri) {
        this.versionIri = versionIri;
    }

    public String getVersionInfo() {
        return versionInfo;
    }

    public void setVersionInfo(String versionInfo) {
        this.versionInfo = versionInfo;
    }
}