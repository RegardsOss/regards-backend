/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.theia;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.IOpenIdConnectClient;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.request.OpenIdTokenRequest;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.response.OpenIdTokenResponse;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.theia.response.TheiaOpenIdUserInfoResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestClient(name = "theia-open-id-connect", contextId = "theia-open-id-connect")
@RequestMapping(
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE
)
public interface TheiaOpenIdConnectClient extends IOpenIdConnectClient<TheiaOpenIdUserInfoResponse> {

    @PostMapping
    @ResponseBody
    ResponseEntity<OpenIdTokenResponse> token(@RequestBody OpenIdTokenRequest request);

    @GetMapping
    @ResponseBody
    ResponseEntity<TheiaOpenIdUserInfoResponse> userInfo();

    @PostMapping
    ResponseEntity<Void> revoke(@RequestParam("token") String token);
}
