/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.utils.jwt;

import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;

/**
 * Utility class for working with tenants
 *
 * @author Xavier-Alexandre Brochard
 * @author Séastien Binda
 */
public final class JwtTokenUtils {

    /**
     * Class logger
     */
    public static final Logger LOG = LoggerFactory.getLogger(JwtTokenUtils.class);

    private JwtTokenUtils() {
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
    public static <T> Function<String, T> asSafeCallableOnTenant(final Supplier<T> pSupplier) {
        // Return a predicate taking a tenant as argument and which....
        return pTenant -> {
            // ...records the inital tenant
            final JWTAuthentication auth = (JWTAuthentication) SecurityContextHolder.getContext().getAuthentication();
            final String intialTenant = auth.getUser().getTenant();
            // ...switches to the tenant
            auth.getUser().setTenant(pTenant);
            SecurityContextHolder.getContext().setAuthentication(auth);
            // ...evaluates the boolean supplier
            final T result = pSupplier.get();
            // ... and switches back to original tenant
            auth.getUser().setTenant(intialTenant);
            SecurityContextHolder.getContext().setAuthentication(auth);
            return result;
        };
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
    public static <T> Function<String, T> asSafeCallableOnRole(final Supplier<T> pSupplier,
            final JWTService pJwtService) {
        // Return a predicate taking a tenant as argument and which....
        return pRole -> {
            T result = null;
            // ...records the inital tenant
            final JWTAuthentication initialToken = (JWTAuthentication) SecurityContextHolder.getContext()
                    .getAuthentication();
            // ... generate new token
            try {
                pJwtService.injectToken(initialToken.getProject(), pRole);
                // ...evaluates the boolean supplier
                result = pSupplier.get();
            } catch (final JwtException e) {
                LOG.error(e.getMessage(), e);
            }
            // ... and switches back to original tenant
            SecurityContextHolder.getContext().setAuthentication(initialToken);

            return result;
        };
    }
}
