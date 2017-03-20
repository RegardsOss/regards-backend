/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.utils.jwt;

import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class to extract information from token
 *
 * @author Marc Sordi
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
     * If no security context found, inject one with specified role. This method must not be used in production and is
     * supplied for test purpose only.
     *
     * @param role
     *            role to mock
     */
    public static void mockActualRole(String role) {
        JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            authentication = new JWTAuthentication(null);
            UserDetails details = new UserDetails();
            details.setRole(role);
            authentication.setUser(details);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }
}
