package fr.cnes.regards.modules.accessrights.instance.domain.passwordreset;

/**
 * Dto class wrapping data required for the {@link AccountsController#requestResetPassword} endpoint.
 *
 * @author Xavier-Alexandre Brochard
 */
public class RequestResetPasswordDto {

    /**
     * The token
     */
    private String originUrl;

    /**
     * The new password
     */
    private String requestLink;

    /**
     * @param pOriginUrl
     * @param pResetUrl
     */
    public RequestResetPasswordDto(final String pOriginUrl, final String pRequestLink) {
        super();
        originUrl = pOriginUrl;
        requestLink = pRequestLink;
    }

    /**
     * @return the originUrl
     */
    public String getOriginUrl() {
        return originUrl;
    }

    /**
     * @param pOriginUrl
     *            the originUrl to set
     */
    public void setOriginUrl(final String pOriginUrl) {
        originUrl = pOriginUrl;
    }

    public String getRequestLink() {
        return requestLink;
    }

    public void setRequestLink(final String pRequestLink) {
        requestLink = pRequestLink;
    }

}
