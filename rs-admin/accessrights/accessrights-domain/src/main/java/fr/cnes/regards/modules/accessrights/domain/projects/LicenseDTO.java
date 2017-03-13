/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class LicenseDTO {

    /**
     * whether the current user has accepted or not the licence
     */
    private boolean accepted;

    /**
     * URL to download the licence
     */
    private String licenseLink;

    protected LicenseDTO() {
        // for (de)serialization
    }

    public LicenseDTO(boolean pAccepted, String pLicenceLink) {
        super();
        accepted = pAccepted;
        licenseLink = pLicenceLink == null ? "" : pLicenceLink;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean pAccepted) {
        accepted = pAccepted;
    }

    public String getLicenceLink() {
        return licenseLink;
    }

    public void setLicenceLink(String pLicenceLink) {
        licenseLink = pLicenceLink == null ? "" : pLicenceLink;
    }

}
