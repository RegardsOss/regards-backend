package fr.cnes.regards.framework.security.filter;

import fr.cnes.regards.framework.security.utils.HttpConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.authentication.www.BasicAuthenticationConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Check that "Basic" Authorization header is valid for authentication endpoint (i.e. ".../oauth/token...")
 * @author Olivier Rousselot
 */
public class BasicAuthenticationFilter  extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicAuthenticationFilter.class);

    private String clientUser;

    private String clientSecret;

    public BasicAuthenticationFilter(String clientUser, String clientSecret) {
        this.clientUser = clientUser;
        this.clientSecret = clientSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        // Retrieve authentication header
        String authHeader = request.getHeader(HttpConstants.AUTHORIZATION);
        boolean authenticationEndpoint = request.getRequestURI().contains("/oauth/token");
        if (authenticationEndpoint) {
            if (authHeader == null) {
                throw new BadCredentialsException("Authentication required");
            }
            authHeader = authHeader.trim();
            // Check that it is a "Basic" authentication...
            if (!StringUtils.startsWithIgnoreCase(authHeader,
                                                 BasicAuthenticationConverter.AUTHENTICATION_SCHEME_BASIC)) {
                throw new BadCredentialsException("Basic Authorization required");
            }
            // ...and that it contains user/password encoded values
            if (authHeader.equalsIgnoreCase(BasicAuthenticationConverter.AUTHENTICATION_SCHEME_BASIC)) {
                throw new BadCredentialsException("Empty Basic Authorization token");
            }
            // Decode user/password
            byte[] base64Token = authHeader.substring(6).getBytes(StandardCharsets.UTF_8);
            byte[] decoded = decode(base64Token);
            String token = new String(decoded, StandardCharsets.UTF_8);
            int delim = token.indexOf(":");
            if (delim == -1) {
                throw new BadCredentialsException("Invalid Basic Authorization token");
            }
            String userFromBasic = token.substring(0, delim);
            String secretFromBasic = token.substring(delim + 1);
            if (!userFromBasic.equals(clientUser) || !secretFromBasic.equals(clientSecret)) {
                throw new BadCredentialsException("Invalid Basic Authorization token");
            }
        }
        filterChain.doFilter(request, response);
    }

    private byte[] decode(byte[] base64Token) {
        try {
            return Base64.getDecoder().decode(base64Token);
        } catch (IllegalArgumentException ex) {
            throw new BadCredentialsException("Failed to decode basic authentication token");
        }
    }

}
