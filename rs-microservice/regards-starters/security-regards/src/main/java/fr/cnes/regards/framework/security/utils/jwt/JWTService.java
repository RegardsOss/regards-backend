/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
     * Email claim
     */
    public static final String CLAIM_EMAIL = "email";

    /**
     * Encryption algorithm
     */
    private static final SignatureAlgorithm ALGO = SignatureAlgorithm.HS512;

    /**
     * Short Encryption algorithm (for user specific token generation)
     */
    private static final SignatureAlgorithm SHORT_ALGO = SignatureAlgorithm.HS256;

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(JWTService.class);

    /**
     * JWT Secret. Default value is only useful for testing purpose.
     */
    @Value("${jwt.secret:123456789}")
    private String secret;

    /**
     * validity delay expressed in minutes. Defaults to 120.
     */
    @Value("${jwt.validityDelay:120}")
    private long validityDelay = 120;

    /**
     * Inject a generated token in the {@link SecurityContextHolder}
     * @param tenant tenant
     * @param role Role name
     * @param user User name
     * @param email User email
     * @throws JwtException Error during token generation
     * @since 1.0-SNAPSHOT
     */
    public void injectToken(final String tenant, final String role, final String user, final String email)
            throws JwtException {
        final String token = generateToken(tenant, user, email, role);
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
        jwt.setUser(new UserDetails(pTenant, "MockName", "Mock@mail", pRole));
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

            final String tenant = claims.getBody().get(CLAIM_TENANT, String.class);
            if (tenant == null) {
                LOG.error("The tenant cannot be null");
                throw new MissingClaimException(CLAIM_TENANT);
            }

            final String login = claims.getBody().getSubject();
            if (login == null) {
                LOG.error("The subject cannot be null");
                throw new MissingClaimException(CLAIM_SUBJECT);
            }

            final String role = claims.getBody().get(CLAIM_ROLE, String.class);
            if (role == null) {
                LOG.error("The role cannot be null");
                throw new MissingClaimException(CLAIM_ROLE);
            }

            final String email = claims.getBody().get(CLAIM_EMAIL, String.class);
            if (email == null) {
                LOG.error("The email cannot be null");
                throw new MissingClaimException(CLAIM_EMAIL);
            }

            pAuthentication.setUser(new UserDetails(tenant, email, login, role));

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
     * @param user user name
     * @param email user email
     * @param role user role
     * @return a Json Web Token
     */
    public String generateToken(String tenant, String user, String email, String role) {
        return Jwts.builder().setIssuer("regards").setClaims(generateClaims(tenant, role, user, email)).setSubject(user)
                .signWith(ALGO, TextCodec.BASE64.encode(secret))
                .setExpiration(Date.from(OffsetDateTime.now().plusMinutes(validityDelay).toInstant())).compact();
    }

    /**
     * FIXME : JWT should be completed with expiration date
     *
     * FIXME : JWT generate must manage RSA keys
     *
     * Generate a JWT handling the tenant name, the user name and its related role
     * @param tenant tenant
     * @param userLoginAndMail user name & mail
     * @param role user role
     * @return a Json Web Token
     */
    public String generateToken(String tenant, String userLoginAndMail, String role) {
        return this.generateToken(tenant, userLoginAndMail, userLoginAndMail, role);
    }

    /**
     * Generate a token providing almost all informations
     * @param tenant tenant
     * @param user user who aked for token
     * @param email user email
     * @param role user role
     * @param expirationDate specific expiration date
     * @param additionalParams additional parameters (user specific)
     * @param secret sec ret phrase (user specific)
     * @param shorter if true, use a 256 bits algo instead of 512
     * @return a Json Web Token
     */
    public String generateToken(String tenant, String user, String email, String role, OffsetDateTime expirationDate,
            Map<String, Object> additionalParams, String secret, boolean shorter) {
        return Jwts.builder().setIssuer("regards")
                .setClaims(generateClaims(tenant, role, user, email, additionalParams)).setSubject(user)
                .signWith(shorter ? SHORT_ALGO : ALGO, TextCodec.BASE64.encode(secret))
                .setExpiration(Date.from(expirationDate.toInstant())).compact();
    }

    /**
     * Decode token and returns claims
     * @param token token to decode
     * @param secret secret used to generate it
     * @throws InvalidJwtException
     * @return parsed {@link Claims}
     */
    public Claims parseToken(String token, String secret) throws InvalidJwtException {
        try {
            return Jwts.parser().setSigningKey(TextCodec.BASE64.encode(secret)).parseClaimsJws(token).getBody();
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
     * @param role user role
     * @param login user name
     * @param email user email
     * @return claim map
     * @since 1.0-SNAPSHOT
     */
    public Map<String, Object> generateClaims(final String tenant, final String role, final String login,
            final String email) {
        return generateClaims(tenant, role, login, email, null);
    }

    /**
     * Method to generate REGARDS JWT Tokens CLAIMS
     * @param tenant tenant
     * @param role user role
     * @param login user name
     * @param email user email
     * @param additionalParams optional additional parameters (can be null)
     * @return claim map
     */
    public Map<String, Object> generateClaims(final String tenant, final String role, final String login, String email,
            Map<String, Object> additionalParams) {
        final Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TENANT, tenant);
        claims.put(CLAIM_ROLE, role);
        claims.put(CLAIM_SUBJECT, login);
        claims.put(CLAIM_EMAIL, email);
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
