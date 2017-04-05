/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.utils.jwt;

import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class to extract information from the {@link JWTAuthentication} token.
 *
 * @author Marc Sordi
 * @author Christophe Mertz
 *
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * @return role hold by security context
     */
    public static String getActualRole() {
        JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext().getAuthentication();
        if ((authentication != null) && (authentication.getUser() != null)) {
            return authentication.getUser().getRole();
        } else {
            return null;
        }
    }

    /**
     * If any security context found, inject one with specified role.</br>
     * This method must not be used in production and is supplied for test purpose only.
     *
     * @param role
     *            role to mock
     */
    // FIXME trouver un autre moyen de mocker le role / Voir powermock
    public static void mockActualRole(String role) {
        JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            authentication = new JWTAuthentication(null);
            UserDetails details = new UserDetails();
            details.setRole(role);
            authentication.setUser(details);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            authentication.getUser().setRole(role);
        }
    }

    /**
     * Get the user name from the {@link JWTAuthentication}.<</br>
     * If any security context found, return null.  
     * 
     * @return the user name
     */
    public static String getActualUser() {
        JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext().getAuthentication();
        if ((authentication != null) && (authentication.getUser() != null)) {
            return authentication.getUser().getName();
        } else {
            return null;
        }
    }
}
