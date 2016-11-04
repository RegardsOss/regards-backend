/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.configuration;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.authentication.TokenExtractor;

import fr.cnes.regards.framework.security.domain.HttpConstants;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;

/**
 *
 * Class JwtTokenExtractor
 *
 * Class to extract informations from JWT token
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class JwtTokenExtractor implements TokenExtractor {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(JwtTokenExtractor.class);

    /**
     * Security service
     */
    private final JWTService jwtService;

    public JwtTokenExtractor(final JWTService pJwtService) {
        super();
        jwtService = pJwtService;
    }

    @Override
    public Authentication extract(final HttpServletRequest pRequest) {
        String jwt = pRequest.getHeader(HttpConstants.AUTHORIZATION);
        jwt = jwt.substring(HttpConstants.BEARER.length()).trim();
        JWTAuthentication authentication = new JWTAuthentication(jwt);
        try {
            authentication = jwtService.parseToken(authentication);
        } catch (final JwtException e) {
            LOG.error(e.getMessage(), e);
        }
        return authentication;
    }
}
