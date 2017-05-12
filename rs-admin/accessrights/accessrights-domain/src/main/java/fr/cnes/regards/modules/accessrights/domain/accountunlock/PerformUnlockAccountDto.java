/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.accountunlock;

/**
 *
 * Class PerformUnlockAccountDto
 *
 * POJO for REST interface to unlock an account by using a given token
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class PerformUnlockAccountDto {

    /**
     * Token to unlock account
     */
    private String token = "";

    public String getToken() {
        return token;
    }

    public void setToken(final String pToken) {
        token = pToken;
    }

}
