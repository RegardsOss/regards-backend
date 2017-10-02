/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.security.utils.jwt;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.security.utils.jwt.exception.InvalidJwtException;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.framework.security.utils.jwt.exception.MissingClaimException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.impl.TextCodec;

/**
 * Utility service based on JJWT library to generate or part a JWT based on a secret.
 * @author Marc Sordi
 * @author Christophe Mertz
 */
@Service
public class JWTService {

    /**
     * Tenant claim
     */
    public static final String CLAIM_TENANT = "tenant";

    /**
     * Role claim
     */
    public static final String CLAIM_ROLE = "role";

    /**
     * Subject claim
     */
    public static final String CLAIM_SUBJECT = "sub";

    /**
     * Encryption algorithm
     */
    private static final SignatureAlgorithm ALGO = SignatureAlgorithm.HS512;

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(JWTService.class);

    /**
     * JWT Secret
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * validity delay expressed in minutes. Defaults to 120.
     */
    @Value("${jwt.validityDelay:120}")
    private long validityDelay = 120;

    /**
     * Inject a generated token in the {@link SecurityContextHolder}
     * @param pTenant tenant
     * @param pRole Role name
     * @param pUserName User name
     * @throws JwtException Error during token generation
     * @since 1.0-SNAPSHOT
     */
    public void injectToken(final String pTenant, final String pRole, final String pUserName) throws JwtException {
        final String token = generateToken(pTenant, pUserName, pRole);
        injectToken(token);
    }

    /**
     * Inject a generated token in the {@link SecurityContextHolder}
     * @param pToken the token to inject into the {@link SecurityContextHolder}
     * @throws JwtException Error during token parsing
     * @since 1.2-SNAPSHOT
     */
    private void injectToken(final String pToken) throws JwtException {
        final JWTAuthentication auth = parseToken(new JWTAuthentication(pToken));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * Mock to simulate a token in the {@link SecurityContextHolder}.
     * @param pTenant tenant
     * @param pRole Role name
     * @since 1.0-SNAPSHOT
     */
    public void injectMockToken(final String pTenant, final String pRole) {
        final JWTAuthentication jwt = new JWTAuthentication("mockJWT"); // Unparseable token
        final UserDetails details = new UserDetails();
        details.setTenant(pTenant);
        details.setName("MockName");
        details.setName("Mock@mail");
        jwt.setUser(details);
        jwt.setRole(pRole);
        jwt.setAuthenticated(Boolean.TRUE);
        SecurityContextHolder.getContext().setAuthentication(jwt);
    }

    /**
     * Parse JWT to retrieve full user information
     * @param pAuthentication containing just JWT
     * @return Full user information
     * @throws JwtException Invalid JWT signature
     */
    public JWTAuthentication parseToken(final JWTAuthentication pAuthentication) throws JwtException {

        try {
            final Jws<Claims> claims = Jwts.parser().setSigningKey(TextCodec.BASE64.encode(secret))
                    .parseClaimsJws(pAuthentication.getJwt());
            // OK, trusted JWT parsed and validated

            final UserDetails user = new UserDetails();

            final String tenant = claims.getBody().get(CLAIM_TENANT, String.class);
            if (tenant == null) {
                LOG.error("The tenant cannot be null");
                throw new MissingClaimException(CLAIM_TENANT);
            }
            user.setTenant(tenant);

            final String name = claims.getBody().getSubject();
            if (name == null) {
                LOG.error("The subject cannot be null");
                throw new MissingClaimException(CLAIM_SUBJECT);
            }
            user.setName(name);

            pAuthentication.setUser(user);

            final String role = claims.getBody().get(CLAIM_ROLE, String.class);
            if (role == null) {
                LOG.error("The role cannot be null");
                throw new MissingClaimException(CLAIM_ROLE);
            }
            pAuthentication.setRole(role);

            pAuthentication.setAuthenticated(Boolean.TRUE);

            return pAuthentication;
        } catch (final SignatureException e) {
            final String message = "JWT signature validation failed";
            LOG.error(message, e);
            throw new InvalidJwtException(message);
        }
    }

    /**
     * FIXME : JWT should be completed with expiration date
     *
     * FIXME : JWT generate must manage RSA keys
     *
     * Generate a JWT handling the tenant name, the user name and its related role
     * @param tenant tenant
     * @param pName user name
     * @param pRole user role
     * @return a Json Web Token
     */
    public String generateToken(String tenant, String pName, String pRole) {
        return Jwts.builder().setIssuer("regards").setClaims(generateClaims(tenant, pRole, pName)).setSubject(pName)
                .signWith(ALGO, TextCodec.BASE64.encode(secret))
                .setExpiration(Date.from(OffsetDateTime.now().plusMinutes(validityDelay).toInstant())).compact();
    }

    /**
     * Generate a token providing almost all informations
     * @param tenant tenant
     * @param user user who aked for token
     * @param role user role
     * @param expirationDate specific expiration date
     * @param additionalParams additional parameters (user specific)
     * @param secret sec ret phrase (user specific)
     * @return a Json Web Token
     */
    public String generateToken(String tenant, String user, String role, OffsetDateTime expirationDate,
            Map<String, String> additionalParams, String secret) {
        return Jwts.builder().setIssuer("regards").setClaims(generateClaims(tenant, role, user, additionalParams))
                .setSubject(user).signWith(ALGO, TextCodec.BASE64.encode(secret))
                .setExpiration(Date.from(expirationDate.toInstant())).compact();
    }

    public Jws<Claims> parseToken(String token, String secret) throws InvalidJwtException {
        try {
            return Jwts.parser().setSigningKey(TextCodec.BASE64.encode(secret)).parseClaimsJws(token);
        } catch (final SignatureException e) {
            final String message = "Invalid token";
            LOG.error(message, e);
            throw new InvalidJwtException(message);
        }
    }

    /**
     * retrieve the current token in place in the security context
     * @return parsed token which is in the security context
     * @throws JwtException if JWT cannot be parsed
     */
    public JWTAuthentication getCurrentToken() throws JwtException {
        JWTAuthentication jwt = (JWTAuthentication) SecurityContextHolder.getContext().getAuthentication();
        return parseToken(jwt);
    }

    /**
     * Method to generate REGARDS JWT Tokens CLAIMS
     * @param tenant tenant
     * @param pRole user role
     * @param pUserName user name
     * @return claim map
     * @since 1.0-SNAPSHOT
     */
    public Map<String, Object> generateClaims(final String tenant, final String pRole, final String pUserName) {
        return generateClaims(tenant, pRole, pUserName, null);
    }

    /**
     * Method to generate REGARDS JWT Tokens CLAIMS
     * @param tenant tenant
     * @param role user role
     * @param user user name
     * @param additionalParams optional additional parameters (can be null)
     * @return claim map
     */
    public Map<String, Object> generateClaims(final String tenant, final String role, final String user,
            Map<String, String> additionalParams) {
        final Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TENANT, tenant);
        claims.put(CLAIM_ROLE, role);
        claims.put(CLAIM_SUBJECT, user);
        if (additionalParams != null) {
            claims.putAll(additionalParams);
        }
        return claims;
    }

    /**
     * @return the secret
     */
    public String getSecret() {
        return secret;
    }

    /**
     * @param pSecret the secret to set
     */
    public void setSecret(final String pSecret) {
        secret = pSecret;
    }

    public void setValidityDelay(long pValidityDelay) {
        validityDelay = pValidityDelay;
    }

}
