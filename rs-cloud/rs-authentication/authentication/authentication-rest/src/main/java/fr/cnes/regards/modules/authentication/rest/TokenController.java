/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.authentication.rest;

import com.google.common.base.Strings;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.authentication.domain.data.Authentication;
import fr.cnes.regards.modules.authentication.domain.dto.ServiceProviderDto;
import fr.cnes.regards.modules.authentication.service.oauth2.Oauth2AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping(path = TokenController.ROOT_PATH_OAUTH)
public class TokenController {

    public static final String ROOT_PATH_OAUTH = "/oauth/";

    public static final String PATH_TOKEN = "token";

    private final Oauth2AuthenticationService oauth2AuthenticationService;

    public TokenController(Oauth2AuthenticationService oauth2AuthenticationService) {
        this.oauth2AuthenticationService = oauth2AuthenticationService;
    }

    /**
     * Useful record for body
     */
    public record UserAuthentication(String username,
                                     String password,
                                     String scope,
                                     String grant_type) {

    }

    /**
     * Endpoint to get a token. Parameters can be passed through request parameters or through body. In case both
     * are used with different values, body property prepends on request parameter.
     * <b>Use of request parameters is deprecated (please use body) and will be removed in next version</b>
     */
    @Operation(summary = "Oauth2 authentication endpoint.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Authentication granted."),
                            @ApiResponse(responseCode = "400", description = "Authentication denied.") })
    @ResourceAccess(role = DefaultRole.PUBLIC, description = "Oauth2 authentication endpoint.")
    @PostMapping(path = PATH_TOKEN)
    public ResponseEntity<Authentication> postAccessToken(
        @Deprecated @RequestParam Map<String, String> requestParameters,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Authentication credentials.",
                                                              content = @Content(schema = @Schema(implementation = ServiceProviderDto.class)))
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
