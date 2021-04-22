package fr.cnes.regards.modules.ingest.domain.sip;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public enum VersioningMode {
    /**
     * This new version is simply ignored, nothing more is being done.
     */
    IGNORE,
    /**
     * This new version is taken into consideration, old versions are still there.
     */
    INC_VERSION,
    /**
     * This new version is to be handled by a human who will decide what to do.
     */
    MANUAL,
    /**
     * This new version will replace the old one.
     * That means old versions will be marked as deleted once the new version has been successfully handled
     */
    REPLACE
}
