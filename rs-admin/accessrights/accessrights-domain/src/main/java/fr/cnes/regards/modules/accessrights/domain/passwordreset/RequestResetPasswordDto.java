package fr.cnes.regards.modules.accessrights.domain.passwordreset;

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
    private String resetUrl;

    /**
     * @param pOriginUrl
     * @param pResetUrl
     */
    public RequestResetPasswordDto(final String pOriginUrl, final String pResetUrl) {
        super();
        originUrl = pOriginUrl;
        resetUrl = pResetUrl;
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

    /**
     * @return the resetUrl
     */
    public String getResetUrl() {
        return resetUrl;
    }

    /**
     * @param pResetUrl
     *            the resetUrl to set
     */
    public void setResetUrl(final String pResetUrl) {
        resetUrl = pResetUrl;
    }

}
