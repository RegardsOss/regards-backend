/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.authentication.plugins.domain;

/**
 *
 * Class ExternalAuthenticationInformations
 *
 * POJO for needed informations to authenticate from an external service provider.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class ExternalAuthenticationInformations {

    /**
     * User name
     */
    private String userName;

    /**
     * Regards project to authenticate to
     */
    private String project;

    /**
     * External authentication ticket from the service provider
     */
    private byte[] ticket;

    /**
     * API Key to authenticate REGARDS to the service provider
     */
    private String providerKey;

    public ExternalAuthenticationInformations() {
        super();
    }

    public ExternalAuthenticationInformations(final String pUserName, final String pProject, final byte[] pTicket,
            final String pProviderKey) {
        super();
        userName = pUserName;
        project = pProject;
        ticket = pTicket;
        providerKey = pProviderKey;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(final String pUserName) {
        userName = pUserName;
    }

    public String getProject() {
        return project;
    }

    public void setProject(final String pProject) {
        project = pProject;
    }

    public byte[] getTicket() {
        return ticket;
    }

    public void setTicket(final byte[] pTicket) {
        ticket = pTicket;
    }

    public String getProviderKey() {
        return providerKey;
    }

    public void setProviderKey(final String pProviderKey) {
        providerKey = pProviderKey;
    }

}
