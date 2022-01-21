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
package fr.cnes.regards.modules.authentication.client;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.authentication.domain.data.Authentication;
import fr.cnes.regards.modules.authentication.domain.dto.ServiceProviderDto;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestClient(name = "rs-authentication", contextId = "rs-authentication.external-authentication-client")
@RequestMapping(
        path = "/serviceproviders",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
)
public interface IExternalAuthenticationClient {

    String ACCEPT_ACCOUNT_RELATIVE_PATH = "/verify";

    @GetMapping(value = ACCEPT_ACCOUNT_RELATIVE_PATH)
    ResponseEntity<Authentication> verifyAndAuthenticate(@RequestParam String externalToken);

    @GetMapping
    ResponseEntity<PagedModel<EntityModel<ServiceProviderDto>>> getServiceProviders();

}
