/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.hibernate;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import fr.cnes.regards.microservices.core.security.jwt.JWTAuthentication;

/**
 *
 * Spring component used by Hibernante to determine the tenant to use during the datasource connection creation.
 *
 *
 * @author CS
 * @since 1.0-SNAPSHOT.
 */
@Component
@ConditionalOnProperty("microservice.dao.enabled")
public class CurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver {

    private static final String DEFAULT_TENANT = "default";

    @Override
    public String resolveCurrentTenantIdentifier() {
        JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getProject();
        }
        else {
            return DEFAULT_TENANT;
        }
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
