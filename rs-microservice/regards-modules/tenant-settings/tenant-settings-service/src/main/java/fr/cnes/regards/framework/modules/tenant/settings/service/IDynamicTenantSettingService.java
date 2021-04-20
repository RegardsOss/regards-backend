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
package fr.cnes.regards.framework.modules.tenant.settings.service;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;

import java.util.Optional;
import java.util.Set;

public interface IDynamicTenantSettingService {

    DynamicTenantSetting create(DynamicTenantSetting dynamicTenantSetting) throws EntityOperationForbiddenException, EntityInvalidException, EntityNotFoundException;

    Optional<DynamicTenantSetting> read(String name);

    Set<DynamicTenantSetting> readAll(Set<String> nameList);

    Set<DynamicTenantSetting> readAll();

    <T> DynamicTenantSetting update(String name, T value) throws EntityNotFoundException, EntityOperationForbiddenException, EntityInvalidException;

    void delete(String name) throws EntityNotFoundException;

    void reset(String name) throws EntityNotFoundException, EntityInvalidException, EntityOperationForbiddenException;

}
