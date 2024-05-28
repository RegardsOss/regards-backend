package fr.cnes.regards.modules.authentication.rest;

import com.google.common.base.Strings;
import fr.cnes.regards.modules.authentication.domain.data.Authentication;
import fr.cnes.regards.modules.authentication.service.oauth2.Oauth2AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller replacing spring security oauth2 token request endpoint.<br/>
 * The goal is to remain compatible with current OAuth2 token request.<br/>
 *
 * @author Olivier Rousselot
 */
@RestController
@RequestMapping(path = "/oauth")
public class TokenController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenController.class);

    private final Oauth2AuthenticationService oauth2AuthenticationService;

    /**
     * Client login
     */


    public TokenController(Oauth2AuthenticationService oauth2AuthenticationService) {
        this.oauth2AuthenticationService = oauth2AuthenticationService;
    }

    /**
     * Only POST is allowed
     */
    @RequestMapping(value = "/token", method = RequestMethod.GET)
    public ResponseEntity<Authentication> getAccessToken(@RequestParam Map<String, String> parameters)
        throws HttpRequestMethodNotSupportedException {
        throw new HttpRequestMethodNotSupportedException("GET");
    }

    /**
     * Useful record for body
     */
    public record UserAuthentication(String username, String password, String scope, String grant_type) { }

    /**
     * Endpoint to get a token. Parameters can be passed through request parameters or through body. In case both
     * are used with different values, body property prepends on request parameter.
     * <b>Use of request parameters is deprecated (please use body) and will be removed in next version</b>
     */
    @PostMapping(path = "/token")
    public ResponseEntity<Authentication> postAccessToken(@Deprecated @RequestParam Map<String, String> requestParameters,
                                                          @RequestBody Optional<UserAuthentication> userAuthenticationOpt) {
        // requestParameters map may or may not contain all user authentication properties.
        // If not, userAuthentication must contain these properties
        UserAuthentication userAuthentication = mergeUserAuthentication(requestParameters, userAuthenticationOpt);

        return getResponse(oauth2AuthenticationService.doAuthentication(userAuthentication.username(),
                                                                        userAuthentication.password(),
                                                                        userAuthentication.scope()));

    }



    /**
     * requestParameters map may or may not contain all user authentication properties.
     * If not, userAuthentication must contain these properties
     *
     * @param requestParameters     from request
     * @param userAuthenticationOpt from body
     * @return merged userAuthentication
     */
    private UserAuthentication mergeUserAuthentication(Map<String, String> requestParameters,
                                                       Optional<UserAuthentication> userAuthenticationOpt) {
        HashMap<String, String> parameters = new HashMap<>(requestParameters);
        if (userAuthenticationOpt.isPresent()) {
            UserAuthentication userAuthentication = userAuthenticationOpt.get();
            // priority is given to properties given into body
            if (!Strings.isNullOrEmpty(userAuthentication.username())) {
                parameters.put("username", userAuthentication.username());
            }
            if (!Strings.isNullOrEmpty(userAuthentication.password())) {
                parameters.put("password", userAuthentication.password());
            }
            if (!Strings.isNullOrEmpty(userAuthentication.scope())) {
                parameters.put("scope", userAuthentication.scope());
            }
            if (!Strings.isNullOrEmpty(userAuthentication.grant_type())) {
                parameters.put("grant_type", userAuthentication.grant_type());
            }
        }
        return new UserAuthentication(parameters.get("username"),
                                      parameters.get("password"),
                                      parameters.get("scope"),
                                      parameters.get("grant_type"));
    }

    private ResponseEntity<Authentication> getResponse(Authentication authentication) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cache-Control", "no-store");
        headers.set("Pragma", "no-cache");
        headers.set("Content-Type", "application/json;charset=UTF-8");
        return new ResponseEntity<>(authentication, headers, HttpStatus.OK);
    }

}
