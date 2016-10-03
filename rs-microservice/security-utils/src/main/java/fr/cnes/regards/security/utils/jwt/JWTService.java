/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.security.utils.jwt;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.cnes.regards.security.utils.jwt.exception.InvalidJwtException;
import fr.cnes.regards.security.utils.jwt.exception.MissingClaimException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;

/**
 *
 * Utility service based on JJWT library to generate or parte a JWT based on a secret.
 *
 * @author msordi
 *
 */
@Service
public class JWTService {

    private static SignatureAlgorithm ALGO = SignatureAlgorithm.HS512;

    private static final String CLAIM_PROJECT = "project";

    private static final String CLAIM_EMAIL = "email";

    private static final String CLAIM_ROLE = "role";

    @Value("${jwt.secret}")
    private String secret_;

    private static final Logger LOG = LoggerFactory.getLogger(JWTService.class);

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
    public JWTAuthentication parseToken(JWTAuthentication pAuthentication)
            throws InvalidJwtException, MissingClaimException {

        try {
            Jws<Claims> claims = Jwts.parser().setSigningKey(secret_).parseClaimsJws(pAuthentication.getJwt());
            // OK, trusted JWT parsed and validated

            UserDetails user = new UserDetails();

            String project = claims.getBody().get(CLAIM_PROJECT, String.class);
            if (project == null) {
                throw new MissingClaimException(CLAIM_PROJECT);
            }
            user.setTenant(project);

            String email = claims.getBody().get(CLAIM_EMAIL, String.class);
            if (email == null) {
                throw new MissingClaimException(CLAIM_EMAIL);
            }
            user.setEmail(email);

            String name = claims.getBody().getSubject();
            if (name == null) {
                throw new MissingClaimException("sub");
            }
            user.setName(name);

            pAuthentication.setUser(user);

            String role = claims.getBody().get(CLAIM_ROLE, String.class);
            if (role == null) {
                throw new MissingClaimException(CLAIM_ROLE);
            }
            pAuthentication.setRole(role);

            pAuthentication.setAuthenticated(Boolean.TRUE);

            return pAuthentication;
        }
        catch (SignatureException e) {
            String message = "JWT signature validation failed";
            LOG.error(message, e);
            throw new InvalidJwtException(message);
        }
    }

    // FIXME : creation must be moved in another place!
    // JWT should be complete with :
    // - expiration date
    // - data access groups
    public String generateToken(String pProject, String pEmail, String pName, String pRole) {
        return Jwts.builder().setIssuer("regards").setClaims(generateClaims(pProject, pEmail, pRole)).setSubject(pName)
                .signWith(ALGO, secret_).compact();
    }

    /**
     *
     * Method to generate REGARDS JWT Tokens CLAIMS
     *
     * @param pProject
     * @param pEmail
     * @param pRole
     * @return
     * @since 1.0-SNAPSHOT
     */
    public Map<String, Object> generateClaims(String pProject, String pEmail, String pRole) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_PROJECT, pProject);
        claims.put(CLAIM_EMAIL, pEmail);
        claims.put(CLAIM_ROLE, pRole);
        return claims;
    }

    /**
     * @return the secret
     */
    public String getSecret() {
        return secret_;
    }

    /**
     * @param pSecret
     *            the secret to set
     */
    public void setSecret(String pSecret) {
        secret_ = pSecret;
    }
}
