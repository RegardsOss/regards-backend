/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.hibernate;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
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
public class CurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver {

    @Override
    public String resolveCurrentTenantIdentifier() {
        JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext().getAuthentication();
        return authentication.getProject();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
