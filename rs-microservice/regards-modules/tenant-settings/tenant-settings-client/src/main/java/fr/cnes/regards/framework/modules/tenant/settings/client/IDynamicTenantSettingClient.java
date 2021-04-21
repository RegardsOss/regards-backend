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
package fr.cnes.regards.framework.modules.tenant.settings.client;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;

/**
 * This is the base of the feign client of a module that is being used in multiple microservices. As so, we cannot entirely create the feign client. It has to be create in each microservice using it.
 */
public interface IDynamicTenantSettingClient {

    String ROOT_PATH = "/settings";

    String UPDATE_PATH = "/{name}";

    @PutMapping(path = ROOT_PATH + UPDATE_PATH)
    ResponseEntity<EntityModel<DynamicTenantSetting>> update(@PathVariable(name = "name") String name,
            @RequestBody DynamicTenantSetting setting);

    @GetMapping(path = ROOT_PATH)
    ResponseEntity<List<EntityModel<DynamicTenantSetting>>> retrieveAll(
            @RequestParam(name = "names") Set<String> names);

    default ResponseEntity<List<EntityModel<DynamicTenantSetting>>> retrieveAll() {
        return retrieveAll(null);
    }

    static Map<String, DynamicTenantSetting> transformToMap(List<EntityModel<DynamicTenantSetting>> responseBoby) {
        return HateoasUtils.unwrapCollection(responseBoby).stream().collect(Collectors.toMap(DynamicTenantSetting::getName, Function
                .identity()));
    }
}
