package fr.cnes.regards.cloud.gateway.authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.stereotype.Component;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;

import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.Role;
import fr.cnes.regards.modules.accessRights.signature.IAccessRightsSignature;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Component
public class Oauth2AuthenticationManager implements AuthenticationManager {

    private static final Logger LOG = LoggerFactory.getLogger(Oauth2AuthenticationManager.class);

    @Value("${jwt.secret}")
    private String secret_;

    @Autowired
    private EurekaClient discoveryClient_;

    // TODO : REMOVE and user JwtService from security-utils
    private static SignatureAlgorithm ALGO = SignatureAlgorithm.HS512;

    private static final String CLAIM_PROJECT = "project";

    private static final String CLAIM_EMAIL = "email";

    private static final String CLAIM_ROLE = "role";

    private String generateToken(String pProject, String pEmail, String pName, String pRole) {
        return Jwts.builder().setIssuer("regards").setSubject(pName).claim(CLAIM_PROJECT, pProject)
                .claim(CLAIM_EMAIL, pEmail).claim(CLAIM_ROLE, pRole).signWith(ALGO, secret_).compact();
    }

    @Override
    public Authentication authenticate(Authentication pAuthentication) throws AuthenticationException {
        final String name = pAuthentication.getName();
        final String password = pAuthentication.getCredentials().toString();

        final Object details = pAuthentication.getDetails();
        String scope = null;
        if (details instanceof Map) {
            @SuppressWarnings("unchecked")
            final Map<String, String> detailsMap = (Map<String, String>) details;
            scope = detailsMap.get("scope");
        }
        else {
            final String message = "Invalid scope";
            LOG.error(message);
            throw new OAuth2Exception(message);
        }

        authenticate(name, password, scope);

        ProjectUser user = retreiveUser(name, scope);

        final List<GrantedAuthority> grantedAuths = new ArrayList<>();
        grantedAuths.add(new SimpleGrantedAuthority(user.getRole().getName()));

        return new UsernamePasswordAuthenticationToken(name, password, grantedAuths);

    }

    // TODO : To be implemented in authentication plugin
    private ProjectUser retreiveUser(String pName, String pScope) {
        ProjectUser user = new ProjectUser();
        Role role = new Role();
        role.setName("USER");
        user.setRole(role);
        return user;
    }

    // TODO : To be implemented in authentication plugin
    private void authenticate(String pName, String pPassword, String pScope) {
        LOG.info("Trying to authenticate user " + pName + " with password=" + pPassword + " for project " + pScope);

        final List<GrantedAuthority> grantedAuths = new ArrayList<>();
        grantedAuths.add(new SimpleGrantedAuthority("USER"));

        String token = generateToken(pScope, "plopmail@plop.fr", pName, "USER");

        // 1. Get rs-admin microservice adress
        InstanceInfo instance = discoveryClient_.getNextServerFromEureka("rs-admin", false);
        String adminUrl = instance.getHomePageUrl();

        // Call to the admin service with a generated token to get user requested for the authentication
        IAccessRightsSignature.getClient(adminUrl, token).retrieveProjectUserList();

    }

}
