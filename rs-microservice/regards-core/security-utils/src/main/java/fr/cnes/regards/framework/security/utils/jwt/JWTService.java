/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.utils.jwt;

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
 *
 * Utility service based on JJWT library to generate or parte a JWT based on a secret.
 *
 * @author msordi
 *
 */
@Service
public class JWTService {

    /**
     * Crypting algorithm
     */
    private static SignatureAlgorithm ALGO = SignatureAlgorithm.HS512;

    /**
     * Project claim
     */
    public static final String CLAIM_PROJECT = "project";

    /**
     * Email claim
     */
    public static final String CLAIM_EMAIL = "email";

    /**
     * Role claim
     */
    public static final String CLAIM_ROLE = "role";

    /**
     * Subject claim
     */
    public static final String CLAIM_SUBJECT = "sub";

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(JWTService.class);

    /**
     * TODO
     */
    private static Map<String, String> scopesTokensMap_ = new HashMap<>();

    /**
     * JWT Secret
     */
    @Value("${jwt.secret}")
    private String secret;

    public String getActualTenant() {
        return ((JWTAuthentication) SecurityContextHolder.getContext().getAuthentication()).getTenant();
    }

    public void injectToken(final String pTenant, final String pRole) throws JwtException {
        String token = null;
        if (scopesTokensMap_.get(pTenant) != null) {
            token = scopesTokensMap_.get(pTenant);
        } else {
            token = generateToken(pTenant, "", "", pRole);
        }
        final JWTAuthentication auth = parseToken(new JWTAuthentication(token));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

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
     *
     * @param pAuthentication
     *            containing just JWT
     * @return Full user information
     * @throws MissingClaimException
     *             JWT claim missing
     * @throws InvalidJwtException
     *             Invalid JWT signature
     */
    public JWTAuthentication parseToken(final JWTAuthentication pAuthentication) throws JwtException {

        try {
            final Jws<Claims> claims = Jwts.parser().setSigningKey(TextCodec.BASE64.encode(secret))
                    .parseClaimsJws(pAuthentication.getJwt());
            // OK, trusted JWT parsed and validated

            final UserDetails user = new UserDetails();

            final String project = claims.getBody().get(CLAIM_PROJECT, String.class);
            if (project == null) {
                throw new MissingClaimException(CLAIM_PROJECT);
            }
            user.setTenant(project);

            final String email = claims.getBody().get(CLAIM_EMAIL, String.class);
            if (email == null) {
                throw new MissingClaimException(CLAIM_EMAIL);
            }
            user.setEmail(email);

            final String name = claims.getBody().getSubject();
            if (name == null) {
                throw new MissingClaimException(CLAIM_SUBJECT);
            }
            user.setName(name);

            pAuthentication.setUser(user);

            final String role = claims.getBody().get(CLAIM_ROLE, String.class);
            if (role == null) {
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

    // FIXME : creation must be moved in another place!
    // JWT should be completed with :
    // - expiration date
    // - data access groups
    public String generateToken(final String pProject, final String pEmail, final String pName, final String pRole) {
        return Jwts.builder().setIssuer("regards").setClaims(generateClaims(pProject, pEmail, pRole, pName))
                .setSubject(pName).signWith(ALGO, TextCodec.BASE64.encode(secret)).compact();
    }

    /**
     *
     * Method to generate REGARDS JWT Tokens CLAIMS
     *
     * @param pProject
     *            project name
     * @param pEmail
     *            user email
     * @param pRole
     *            user role
     * @param pUserName
     *            user name
     * @return claim map
     * @since 1.0-SNAPSHOT
     */
    public Map<String, Object> generateClaims(final String pProject, final String pEmail, final String pRole,
            final String pUserName) {
        final Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_PROJECT, pProject);
        claims.put(CLAIM_EMAIL, pEmail);
        claims.put(CLAIM_ROLE, pRole);
        claims.put(CLAIM_SUBJECT, pUserName);
        return claims;
    }

    /**
     * @return the secret
     */
    public String getSecret() {
        return secret;
    }

    /**
     * @param pSecret
     *            the secret to set
     */
    public void setSecret(final String pSecret) {
        secret = pSecret;
    }

}
