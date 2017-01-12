/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.multitenant;

/**
 * In a request context, this resolver allows to retrieve request tenant.
 *
 * @author Marc Sordi
 *
 */
@FunctionalInterface
public interface IThreadTenantResolver {

    String getTenant();
}
