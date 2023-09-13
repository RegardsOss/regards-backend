/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.encryption.SensitiveDynamicSettingConverter;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service to handle {@link DynamicTenantSetting}s with a mask on sensitive values.
 *
 * @author Iliana Ghazali
 **/
@RegardsTransactional
public class DynamicTenantSettingWithMaskService implements IDynamicTenantSettingService {

    private final DynamicTenantSettingService dynamicTenantSettingService;

    private final DynamicTenantSettingRepositoryService dynamicTenantSettingRepositoryService;

    private final SensitiveDynamicSettingConverter sensitiveDynamicSettingConverter;

    public DynamicTenantSettingWithMaskService(DynamicTenantSettingService dynamicTenantSettingService,
                                               DynamicTenantSettingRepositoryService dynamicTenantSettingRepositoryService,
                                               SensitiveDynamicSettingConverter sensitiveDynamicSettingConverter) {
        this.dynamicTenantSettingService = dynamicTenantSettingService;
        this.dynamicTenantSettingRepositoryService = dynamicTenantSettingRepositoryService;
        this.sensitiveDynamicSettingConverter = sensitiveDynamicSettingConverter;
    }

    @Override
    public DynamicTenantSetting create(DynamicTenantSetting dynamicTenantSetting)
        throws EntityOperationForbiddenException, EntityInvalidException, EntityNotFoundException {
        return sensitiveDynamicSettingConverter.maskDynamicSettingWithSensitiveValues(dynamicTenantSettingService.create(
            dynamicTenantSetting));
    }

    @Override
    public Optional<DynamicTenantSetting> read(String name) {
        return dynamicTenantSettingRepositoryService.findByName(name, false)
                                                    .map(sensitiveDynamicSettingConverter::maskDynamicSettingWithSensitiveValues);
    }

    @Override
    public Set<DynamicTenantSetting> readAll(Set<String> nameList) {
        return dynamicTenantSettingRepositoryService.findAllByNameIn(nameList, false)
                                                    .stream()
                                                    .map(sensitiveDynamicSettingConverter::maskDynamicSettingWithSensitiveValues)
                                                    .collect(Collectors.toSet());
    }

    @Override
    public Set<DynamicTenantSetting> readAll() {
        return dynamicTenantSettingRepositoryService.findAll(false)
                                                    .stream()
                                                    .map(sensitiveDynamicSettingConverter::maskDynamicSettingWithSensitiveValues)
                                                    .collect(Collectors.toSet());
    }

    @Override
    public <T> DynamicTenantSetting update(String name, T value)
        throws EntityNotFoundException, EntityOperationForbiddenException, EntityInvalidException {
        return sensitiveDynamicSettingConverter.maskDynamicSettingWithSensitiveValues(dynamicTenantSettingService.update(
            name,
            value));
    }

    @Override
    public void delete(String name) throws EntityNotFoundException {
        dynamicTenantSettingService.delete(name);
    }

    @Override
    public DynamicTenantSetting reset(String name)
        throws EntityNotFoundException, EntityInvalidException, EntityOperationForbiddenException {
        return sensitiveDynamicSettingConverter.maskDynamicSettingWithSensitiveValues(dynamicTenantSettingService.reset(
            name));
    }

    @Override
    public boolean canUpdate(String name) {
        return dynamicTenantSettingService.canUpdate(name);
    }

}
