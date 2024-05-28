/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.security.utils.jwt.exception.InvalidJwtException;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.framework.security.utils.jwt.exception.MissingClaimException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Utility service based on JJWT library to generate or part a JWT based on a secret.
 *
 * @author Marc Sordi
 * @author Christophe Mertz
 */
@Service
public class JWTService implements InitializingBean {

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
     * Access group claim : only used for delegated security to filter metadata & data access
     */
    static final String CLAIM_ACCESS_GROUPS = "accessGroups";

    /**
     * Encryption algorithm
     */
    static final SignatureAlgorithm ALGO = SignatureAlgorithm.HS512;

    /**
     * Short Encryption algorithm (for user specific token generation)
     */
    static final SignatureAlgorithm SHORT_ALGO = SignatureAlgorithm.HS256;

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(JWTService.class);

    /**
     * Validity delay expressed in seconds. Defaults to 7200.
     */
    @Value("${access_token.validity_period:7200}")
    private final long validityDelay = 7200;

    /**
     * Legacy JWT Secret.
     * Use exact algorithm properties instead : {@link #keyForHS256} or/and {@link #keyForHS384} or/and {@link #keyForHS512}
     * At least {@link #keyForHS256} and {@link #keyForHS512} are required!
     */
    @Deprecated
    @Value("${jwt.secret:}")
    private String secret;

    /**
     * Default values is only useful for testing purpose.
     */
    @Value("${jwt.signing-key.HS256:!!!!!==========abcdefghijklmnopqrstuvwxyz0123456789==========!!!!!}")
    private String keyForHS256;

    @Value("${jwt.signing-key.HS384:}")
    private String keyForHS384;

    /**
     * Default values is only useful for testing purpose.
     */
    @Value("${jwt.signing-key.HS512:!!!!!==========abcdefghijklmnopqrstuvwxyz0123456789==========!!!!!}")
    private String keyForHS512;

    @Value("${jwt.signing-key.RS256:}")
    private String keyForRS256;

    @Value("${jwt.signing-key.RS384:}")
    private String keyForRS384;

    @Value("${jwt.signing-key.RS512:}")
    private String keyForRS512;

    /**
     * Key resolver based on algorithm
     */
    private JWTSigningKeyResolver signingKeyResolver;

    /**
     * Key store per algorithm for dynamic resolution
     * SHORT_ALGO & ALGO must be set for internal use.
     */
    private Map<SignatureAlgorithm, String> signingKeys = new HashMap<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        // Initialize store for signing keys
        if (StringUtils.hasText(keyForHS256)) {
            signingKeys.put(SHORT_ALGO, keyForHS256);
        } else {
            if (StringUtils.hasText(secret)) {
                // Propagate legacy property
                signingKeys.put(SHORT_ALGO, secret);
            }
        }
        if (StringUtils.hasText(keyForHS384)) {
            signingKeys.put(SignatureAlgorithm.HS384, keyForHS384);
        }
        if (StringUtils.hasText(keyForHS512)) {
            signingKeys.put(ALGO, keyForHS512);
        } else {
            if (StringUtils.hasText(secret)) {
                // Propagate legacy property
                signingKeys.put(ALGO, secret);
            }
        }
        if (StringUtils.hasText(keyForRS256)) {
            signingKeys.put(SignatureAlgorithm.RS256, keyForRS256);
        }
        if (StringUtils.hasText(keyForRS384)) {
            signingKeys.put(SignatureAlgorithm.RS384, keyForRS384);
        }
        if (StringUtils.hasText(keyForRS512)) {
            signingKeys.put(SignatureAlgorithm.RS512, keyForRS512);
        }
    }

    /**
     * Inject a generated token in the {@link SecurityContextHolder}
     *
     * @param tenant tenant
     * @param role   Role name
     * @param user   User name
     * @param email  User email
     * @throws JwtException Error during token generation
     */
    public void injectToken(final String tenant, final String role, final String user, final String email)
        throws JwtException {
        final String token = generateToken(tenant, user, email, role);
        injectToken(token);
    }

    /**
     * Inject a generated token in the {@link SecurityContextHolder}
     *
     * @param pToken the token to inject into the {@link SecurityContextHolder}
     * @throws JwtException Error during token parsing
     * @since 1.2-SNAPSHOT
     */
    private void injectToken(final String pToken) throws JwtException {
        final JWTAuthentication auth = parseToken(new JWTAuthentication(pToken));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    protected JWTSigningKeyResolver getSigningKeyResolver() {
        if (signingKeyResolver == null) {
            signingKeyResolver = new JWTSigningKeyResolver(signingKeys);
        }
        return signingKeyResolver;
    }

    /**
     * Mock to simulate a token in the {@link SecurityContextHolder}.
     *
     * @param pTenant tenant
     * @param pRole   Role name
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
     *
     * @param authentication containing just JWT
     * @return Full user information
     * @throws JwtException Invalid JWT signature
     */
    public JWTAuthentication parseToken(final JWTAuthentication authentication) throws JwtException {

        Jws<Claims> claims;
        try {
            claims = Jwts.parserBuilder()
                         .setSigningKeyResolver(getSigningKeyResolver())
                         .build()
                         .parseClaimsJws(authentication.getJwt());
            // OK, trusted JWT parsed and validated
        } catch (MalformedJwtException | IllegalArgumentException | SignatureException m) {
            LOG.error("Failed to parse claims");
            throw new InvalidJwtException(m);
        }

        String tenant = claims.getBody().get(CLAIM_TENANT, String.class);
        if (tenant == null) {
            LOG.error("The tenant cannot be null");
            throw new MissingClaimException(CLAIM_TENANT);
        }

        String login = claims.getBody().getSubject();
        if (login == null) {
            LOG.error("The subject cannot be null");
            throw new MissingClaimException(CLAIM_SUBJECT);
        }

        String role = claims.getBody().get(CLAIM_ROLE, String.class);
        if (role == null) {
            LOG.error("The role cannot be null");
            throw new MissingClaimException(CLAIM_ROLE);
        }

        String email = claims.getBody().get(CLAIM_EMAIL, String.class);
        if (email == null) {
            LOG.error("The email cannot be null");
            throw new MissingClaimException(CLAIM_EMAIL);
        }

        UserDetails userDetails = new UserDetails(tenant, email, login, role);

        // Try to retrieve access groups
        List<String> accessGroups = claims.getBody().get(CLAIM_ACCESS_GROUPS, List.class);
        if (accessGroups != null) {
            userDetails.withAccessGroups(new HashSet<>(accessGroups));
        }

        authentication.setUser(userDetails);
        authentication.setAuthenticated(Boolean.TRUE);
        authentication.setAdditionalParams(claims.getBody());
        return authentication;
    }

    /**
     * FIXME : JWT generate must manage RSA keys
     * <p>
     * Generate a JWT handling the tenant name, the user name and its related role
     *
     * @param tenant tenant
     * @param user   username
     * @param email  user email
     * @param role   user role
     * @return a Json Web Token
     */
    public String generateToken(String tenant, String user, String email, String role) {
        return generateToken(tenant, user, email, role, getExpirationDate(OffsetDateTime.now()), null, null, false);
    }

    /**
     * FIXME : JWT generate must manage RSA keys
     * <p>
     * Generate a JWT handling the tenant name, the user name, its related role and additional parameters (user specific)
     *
     * @param tenant           tenant
     * @param user             username
     * @param email            user email
     * @param role             user role
     * @param additionalParams additional parameters (user specific)
     * @return a Json Web Token
     */
    public String generateToken(String tenant,
                                String user,
                                String email,
                                String role,
                                Map<String, Object> additionalParams) {
        return generateToken(tenant,
                             user,
                             email,
                             role,
                             getExpirationDate(OffsetDateTime.now()),
                             additionalParams,
                             null,
                             false);
    }

    /**
     * FIXME : JWT generate must manage RSA keys
     * <p>
     * Generate a JWT handling the tenant name, the user name, its related role and additional parameters (user specific)
     *
     * @param tenant           tenant
     * @param user             username
     * @param email            user email
     * @param role             user role
     * @param expirationDate   specific expiration date
     * @param additionalParams additional parameters (user specific)
     * @return a Json Web Token
     */
    public String generateToken(String tenant,
                                String user,
                                String email,
                                String role,
                                OffsetDateTime expirationDate,
                                Map<String, Object> additionalParams) {
        return generateToken(tenant, user, email, role, expirationDate, additionalParams, null, false);
    }

    /**
     * FIXME : JWT generate must manage RSA keys
     * <p>
     * Generate a JWT handling the tenant name, the user name and its related role
     *
     * @param tenant           tenant
     * @param userLoginAndMail user name & mail
     * @param role             user role
     * @return a Json Web Token
     */
    public String generateToken(String tenant, String userLoginAndMail, String role) {
        return this.generateToken(tenant, userLoginAndMail, userLoginAndMail, role);
    }

    /**
     * Generate a token providing almost all information
     *
     * @param tenant           tenant
     * @param user             user who asked for token
     * @param email            user email
     * @param role             user role
     * @param expirationDate   specific expiration date
     * @param additionalParams additional parameters (user specific)
     * @param shorter          if true, use 256 bits algo instead of 512
     * @return a Json Web Token
     */
    public String generateToken(String tenant,
                                String user,
                                String email,
                                String role,
                                OffsetDateTime expirationDate,
                                Map<String, Object> additionalParams,
                                String secret,
                                boolean shorter) {
        // Resolve secret if not set
        if (secret == null) {
            secret = shorter ? signingKeys.get(SHORT_ALGO) : signingKeys.get(ALGO);
        }
        // THIS METHOD IS NOT USED BY OAUTH2 AUTHENTICATION
        // I.E. NOT USED TO GENERATE TOKENS FOR AUTHENTICATION ON REGARDS PRIVATE USER BASE
        return Jwts.builder()
                   .setIssuer("regards")
                   .setClaims(generateClaims(tenant, role, user, email, additionalParams))
                   .setSubject(user)
                   .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), shorter ? SHORT_ALGO : ALGO)
                   .setExpiration(Date.from(expirationDate.toInstant()))
                   .compact();
    }

    /**
     * Decode token and returns claims
     *
     * @param token token to decode
     * @return parsed {@link Claims}
     */
    final protected Claims parseClaims(String token) throws InvalidJwtException {
        return Jwts.parserBuilder()
                   .setSigningKeyResolver(getSigningKeyResolver())
                   .build()
                   .parseClaimsJws(token)
                   .getBody();
    }

    /**
     * retrieve the current token in place in the security context
     *
     * @return parsed token which is in the security context
     * @throws JwtException if JWT cannot be parsed
     */
    public JWTAuthentication getCurrentToken() throws JwtException {
        JWTAuthentication jwt = (JWTAuthentication) SecurityContextHolder.getContext().getAuthentication();
        return parseToken(jwt);
    }

    /**
     * Method to generate REGARDS JWT Tokens CLAIMS
     *
     * @param tenant tenant
     * @param role   user role
     * @param login  username
     * @param email  user email
     * @return claim map
     */
    public Map<String, Object> generateClaims(final String tenant,
                                              final String role,
                                              final String login,
                                              final String email) {
        return generateClaims(tenant, role, login, email, null);
    }

    /**
     * Method to generate REGARDS JWT Tokens CLAIMS
     *
     * @param tenant           tenant
     * @param role             user role
     * @param login            username
     * @param email            user email
     * @param additionalParams optional additional parameters (can be null)
     * @return claim map
     */
    public Map<String, Object> generateClaims(final String tenant,
                                              final String role,
                                              final String login,
                                              String email,
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

    public OffsetDateTime getExpirationDate(OffsetDateTime generationDate) {
        return generationDate.plusSeconds(validityDelay);
    }

    /**
     * For test purpose only, allows manipulating signing key map
     */
    protected void setSigningKeyFor(SignatureAlgorithm algorithm, String key) {
        signingKeys.put(algorithm, key);
    }

    /**
     * Compatibility method for legacy tests
     */
    public void setSecret(String secret) {
        signingKeys.put(SHORT_ALGO, secret);
        signingKeys.put(ALGO, secret);
    }
}
