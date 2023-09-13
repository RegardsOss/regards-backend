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
package fr.cnes.regards.modules.access.services.rest.user.mock;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import fr.cnes.regards.framework.hateoas.*;
import fr.cnes.regards.framework.jpa.json.GsonUtil;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSettingDto;
import fr.cnes.regards.modules.accessrights.client.IAccessRightSettingClient;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Primary
@Component
public class AccessSettingsClientMock
    implements IAccessRightSettingClient, IResourceController<DynamicTenantSettingDto> {

    public static final List<String> ACCESS_SETTINGS_STUB_GROUPS = Lists.newArrayList("dummy");

    public static final List<DynamicTenantSettingDto> ACCESS_SETTINGS_STUB;

    static {
        GsonUtil.setGson(new Gson());
        ACCESS_SETTINGS_STUB = Arrays.asList(AccessSettings.DEFAULT_GROUPS_SETTING.setValue(ACCESS_SETTINGS_STUB_GROUPS),
                                             AccessSettings.DEFAULT_ROLE_SETTING,
                                             AccessSettings.MODE_SETTING)
                                     .stream()
                                     .map(DynamicTenantSetting::toDto)
                                     .collect(Collectors.toList());
    }

    @Autowired
    private IResourceService resourceService;

    @Override
    public ResponseEntity<List<EntityModel<DynamicTenantSettingDto>>> retrieveAll(Set<String> names) {
        return new ResponseEntity<>(HateoasUtils.wrapList(ACCESS_SETTINGS_STUB), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<EntityModel<DynamicTenantSettingDto>> update(String name, DynamicTenantSettingDto setting) {
        return new ResponseEntity<>(toResource(setting), HttpStatus.OK);
    }

    @Override
    public EntityModel<DynamicTenantSettingDto> toResource(final DynamicTenantSettingDto element,
                                                           final Object... extras) {
        EntityModel<DynamicTenantSettingDto> resource = resourceService.toResource(element);
        resourceService.addLink(resource, this.getClass(), "retrieveAll", LinkRels.SELF);
        resourceService.addLink(resource,
                                this.getClass(),
                                "update",
                                LinkRels.UPDATE,
                                MethodParamFactory.build(String.class, element.getName()),
                                MethodParamFactory.build(AccessSettings.class));

        return resource;
    }
}
