/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.providers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;

import fr.cnes.regards.cloud.gateway.authentication.interfaces.IAuthenticationProvider;
import fr.cnes.regards.modules.accessRights.domain.Account;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.Role;
import fr.cnes.regards.modules.accessRights.domain.UserStatus;
import fr.cnes.regards.modules.accessRights.signature.IAccessRightsSignature;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 *
 * Class SimpleAuthentication
 *
 * TODO : This class should be a REGARDS authentication plugin.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Component
public class SimpleAuthentication implements IAuthenticationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleAuthentication.class);

    @Value("${jwt.secret}")
    private String secret_;

    private static SignatureAlgorithm ALGO = SignatureAlgorithm.HS512;

    public static final String CLAIM_PROJECT = "project";

    public static final String CLAIM_EMAIL = "email";

    public static final String CLAIM_ROLE = "role";

    @Autowired
    private EurekaClient discoveryClient_;

    // TODO : Remove and user the the JwtService from security-utils package
    private String generateToken(String pProject, String pEmail, String pName, String pRole) {
        return Jwts.builder().setIssuer("regards").setSubject(pName).claim(CLAIM_PROJECT, pProject)
                .claim(CLAIM_EMAIL, pEmail).claim(CLAIM_ROLE, pRole).signWith(ALGO, secret_).compact();
    }

    /**
     *
     * Create a sample user.
     *
     *
     * @see fr.cnes.regards.cloud.gateway.authentication.interfaces.IAuthenticationProvider#retreiveUser(java.lang.String,
     *      java.lang.String)
     * @since 1.0-SNPASHOT
     */
    @Override
    public ProjectUser retreiveUser(String pName, String pScope) {
        ProjectUser user = new ProjectUser();
        Role role = new Role();
        role.setName("USER");
        user.setRole(role);
        Account newAccount = new Account();
        newAccount.setEmail("user@regards.c-s.fr");
        user.setAccount(newAccount);
        return user;
    }

    @Override
    public UserStatus authenticate(String pName, String pPassword, String pScope) {
        LOG.info("Trying to authenticate user " + pName + " with password=" + pPassword + " for project " + pScope);

        String token = generateToken(pScope, "", pName, "ADMIN");

        // 1. Get rs-admin microservice adress
        InstanceInfo instance = discoveryClient_.getNextServerFromEureka("rs-admin", false);
        String adminUrl = instance.getHomePageUrl();

        // Call to the admin service with a generated token to get user requested for the authentication
        IAccessRightsSignature.getClient(adminUrl, token).retrieveProjectUserList();

        return UserStatus.ACCESS_GRANTED;

    }

}
