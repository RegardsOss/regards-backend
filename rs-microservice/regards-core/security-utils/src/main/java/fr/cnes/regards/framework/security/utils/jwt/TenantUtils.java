/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.utils.jwt;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class for working with tenants
 *
 * @author Xavier-Alexandre Brochard
 */
public abstract class TenantUtils {

    /**
     * Private constructor to hide default one
     */
    private TenantUtils() {

    }

    /**
     * Decorates a {@link BooleanSupplier} as a {@link Predicate} in order to execute it safeley on a passed tenant.<br>
     * The call will be tenant-safe because it<br>
     * - records the initial tenant<br>
     * - injects the new tenant<br>
     * - switches back to initial tenant at the end<br>
     *
     * @param pTenant
     *            The tenant on which to execute the predicate
     * @param pBooleanSupplier
     *            The decorated boolean supplier
     * @return the decorated predicate
     */
    public static Predicate<String> asSafeCallableOnTenant(final BooleanSupplier pBooleanSupplier) {
        // Return a predicate taking a tenant as argument and which....
        return pTenant -> {
            // ...records the inital tenant
            final JWTAuthentication auth = (JWTAuthentication) SecurityContextHolder.getContext().getAuthentication();
            final String intialTenant = auth.getUser().getTenant();
            // ...switches to the tenant
            auth.getUser().setTenant(pTenant);
            SecurityContextHolder.getContext().setAuthentication(auth);
            // ...evaluates the boolean supplier
            final boolean result = pBooleanSupplier.getAsBoolean();
            // ... and switches back to original tenant
            auth.getUser().setTenant(intialTenant);
            SecurityContextHolder.getContext().setAuthentication(auth);
            return result;
        };
    }
}
