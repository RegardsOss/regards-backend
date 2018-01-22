package fr.cnes.regards.modules.accessrights.instance.domain.passwordreset;

/**
 * Dto class wrapping data required for the {@link AccountsController#performResetPassword} endpoint.
 *
 * @author Xavier-Alexandre Brochard
 */
public class PerformResetPasswordDto {

    /**
     * The token
     */
    private String token;

    /**
     * The new password
     */
    private String newPassword;

    /**
     * @param pToken
     * @param pNewPassword
     */
    public PerformResetPasswordDto(final String pToken, final String pNewPassword) {
        super();
        token = pToken;
        newPassword = pNewPassword;
    }

    /**
     * @return the token
     */
    public String getToken() {
        return token;
    }

    /**
     * @param pToken
     *            the token to set
     */
    public void setToken(final String pToken) {
        token = pToken;
    }

    /**
     * @return the newPassword
     */
    public String getNewPassword() {
        return newPassword;
    }

    /**
     * @param pNewPassword
     *            the newPassword to set
     */
    public void setNewPassword(final String pNewPassword) {
        newPassword = pNewPassword;
    }

}
