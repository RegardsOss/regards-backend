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
 * @author Sylvain Vissiere-Guerinet
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
     * @param pBooleanSupplier
     *            The decorated boolean supplier
     * @return the decorated predicate
     */
    public static <R> Function<String, R> asSafeCallableOnRole(final Supplier<R> pSupplier,
            final JWTService pJwtService) {
        final Function<Void, R> function = pT -> pSupplier.get();
        return asSafeCallableOnRole(function, null, pJwtService);
    }

    /**
     * Decorates a {@link BooleanSupplier} as a {@link Predicate} in order to execute it safeley on a passed tenant.<br>
     * The call will be tenant-safe because it<br>
     * - records the initial tenant<br>
     * - injects the new tenant<br>
     * - switches back to initial tenant at the end<br>
     *
     * @param pFunction
     *            function to execute
     * @param pParameters
     *            parameter(s) of the <code>pFunction</code>, in case of multiple parameter use a collection as
     *            parameter
     * @param pJwtService
     *            service used to handle the jwt
     * @return a {@link Function} executing the <code>pFunction</code> with other privileges than the current one
     */
    public static <T, R> Function<String, R> asSafeCallableOnRole(final Function<T, R> pFunction, T pParameters,
            final JWTService pJwtService) {
        // Return a predicate taking a tenant as argument and which....
        return pRole -> {
            R result = null;
            // ...records the inital tenant
            final JWTAuthentication initialToken = (JWTAuthentication) SecurityContextHolder.getContext()
                    .getAuthentication();
            // ... generate new token
            try {
                pJwtService.injectToken(initialToken.getProject(), pRole);
                // ...evaluates the boolean supplier
                result = pFunction.apply(pParameters);
            } catch (final JwtException e) {
                LOG.error(e.getMessage(), e);
            }
            // ... and switches back to original tenant
            SecurityContextHolder.getContext().setAuthentication(initialToken);

            return result;
        };
    }
}
