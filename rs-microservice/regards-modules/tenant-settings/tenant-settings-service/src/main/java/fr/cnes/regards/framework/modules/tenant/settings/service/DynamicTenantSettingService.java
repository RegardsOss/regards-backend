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
package fr.cnes.regards.framework.modules.tenant.settings.service;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Main service to handle CRUD operations on {@link DynamicTenantSetting}s.
 */
@RegardsTransactional
public class DynamicTenantSettingService implements IDynamicTenantSettingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicTenantSettingService.class);

    private final List<IDynamicTenantSettingCustomizer> dynamicTenantSettingCustomizerList;

    private final DynamicTenantSettingRepositoryService dynamicTenantSettingRepositoryService;

    public DynamicTenantSettingService(List<IDynamicTenantSettingCustomizer> dynamicTenantSettingCustomizerList,
                                       DynamicTenantSettingRepositoryService dynamicTenantSettingRepositoryService) {
        this.dynamicTenantSettingCustomizerList = dynamicTenantSettingCustomizerList;
        this.dynamicTenantSettingRepositoryService = dynamicTenantSettingRepositoryService;
    }

    @Override
    public DynamicTenantSetting create(DynamicTenantSetting dynamicTenantSetting)
        throws EntityOperationForbiddenException, EntityInvalidException, EntityNotFoundException {
        dynamicTenantSetting.setId(null);
        IDynamicTenantSettingCustomizer settingCustomizer = getCustomizer(dynamicTenantSetting);
        checkModelValidity(settingCustomizer, dynamicTenantSetting);
        DynamicTenantSetting savedDynamicTenantSetting = dynamicTenantSettingRepositoryService.save(dynamicTenantSetting,
                                                                                                    null);
        settingCustomizer.doRightNow(dynamicTenantSetting);
        return savedDynamicTenantSetting;
    }

    @Override
    public Optional<DynamicTenantSetting> read(String name) {
        return dynamicTenantSettingRepositoryService.findByName(name, true);
    }

    @Override
    public Set<DynamicTenantSetting> readAll(Set<String> nameList) {
        return dynamicTenantSettingRepositoryService.findAllByNameIn(nameList, true);
    }

    @Override
    public Set<DynamicTenantSetting> readAll() {
        return dynamicTenantSettingRepositoryService.findAll(true);
    }

    @Override
    public <T> DynamicTenantSetting update(String name, T value)
        throws EntityNotFoundException, EntityOperationForbiddenException, EntityInvalidException {
        DynamicTenantSetting dynamicTenantSettingFound = dynamicTenantSettingRepositoryService.findByNameWithExceptionOnNotFound(
            name,
            false);
        IDynamicTenantSettingCustomizer settingCustomizer = getCustomizer(dynamicTenantSettingFound);
        checkAllowedUpdateOperation(settingCustomizer, dynamicTenantSettingFound);
        DynamicTenantSetting updateDynamicTenantSetting;
        if (!Objects.equals(value, dynamicTenantSettingFound.getValue())) {
            updateDynamicTenantSetting = new DynamicTenantSetting(dynamicTenantSettingFound.getId(),
                                                                  dynamicTenantSettingFound.getName(),
                                                                  dynamicTenantSettingFound.getDescription(),
                                                                  dynamicTenantSettingFound.getDefaultValue(),
                                                                  value,
                                                                  dynamicTenantSettingFound.isContainsSensitiveParameters());
            checkModelValidity(settingCustomizer, updateDynamicTenantSetting);
            updateDynamicTenantSetting = dynamicTenantSettingRepositoryService.save(updateDynamicTenantSetting,
                                                                                    dynamicTenantSettingFound);
            settingCustomizer.doRightNow(updateDynamicTenantSetting);
        } else {
            updateDynamicTenantSetting = dynamicTenantSettingFound;
        }
        return updateDynamicTenantSetting;
    }

    @Override
    public void delete(String name) throws EntityNotFoundException {
        DynamicTenantSetting dynamicTenantSetting = dynamicTenantSettingRepositoryService.findByNameWithExceptionOnNotFound(
            name,
            false);
        dynamicTenantSettingRepositoryService.delete(dynamicTenantSetting);
        LOGGER.info("Deleted tenant setting '{}'.", name);
    }

    @Override
    public DynamicTenantSetting reset(String name)
        throws EntityNotFoundException, EntityInvalidException, EntityOperationForbiddenException {
        DynamicTenantSetting dynamicTenantSetting = dynamicTenantSettingRepositoryService.findByNameWithExceptionOnNotFound(
            name,
            false);
        IDynamicTenantSettingCustomizer customizer = getCustomizer(dynamicTenantSetting);
        checkAllowedUpdateOperation(customizer, dynamicTenantSetting);
        if (!dynamicTenantSetting.getDefaultValue().equals(dynamicTenantSetting.getValue())) {
            dynamicTenantSetting.setValue(dynamicTenantSetting.getDefaultValue());
            checkModelValidity(customizer, dynamicTenantSetting);
            dynamicTenantSetting = dynamicTenantSettingRepositoryService.save(dynamicTenantSetting, null);
            customizer.doRightNow(dynamicTenantSetting);
        }
        LOGGER.info("Reset tenant setting '{}'.", dynamicTenantSetting.getName());
        return dynamicTenantSetting;
    }

    @Override
    public boolean canUpdate(String name) {
        try {
            // we are using getCustomizer exception so we do not have to duplicate research logic
            DynamicTenantSetting dynamicTenantSetting = dynamicTenantSettingRepositoryService.findByNameWithExceptionOnNotFound(
                name,
                false);
            checkAllowedUpdateOperation(getCustomizer(dynamicTenantSetting), dynamicTenantSetting);
            return true;
        } catch (EntityInvalidException e) {
            // even if value is invalid(which is highly improbable at this point because we are looking for DB value),
            // we can update the setting
            LOGGER.error(e.getMessage(), e);
            return true;
        } catch (EntityNotFoundException e) {
            // If this occurs it most probably means we could not find the customizer which means we could not update the parameter anyway
            LOGGER.error(e.getMessage(), e);
            return false;
        } catch (EntityOperationForbiddenException e) {
            // If this occurs it means we effectively cannot ute this setting for now per customizer logic
            LOGGER.debug(String.format("Setting %s currently cannot be updated: ", name), e);
            return false;
        }
    }

    private IDynamicTenantSettingCustomizer getCustomizer(DynamicTenantSetting dynamicTenantSetting)
        throws EntityInvalidException, EntityOperationForbiddenException, EntityNotFoundException {
        return dynamicTenantSettingCustomizerList.stream()
                                                 .filter(customizer -> customizer.appliesTo(dynamicTenantSetting))
                                                 .findFirst()
                                                 .orElseThrow(() -> new EntityNotFoundException(dynamicTenantSetting.getName(),
                                                                                                IDynamicTenantSettingCustomizer.class));

    }

    private void checkAllowedUpdateOperation(IDynamicTenantSettingCustomizer settingCustomizer,
                                             DynamicTenantSetting dynamicTenantSetting)
        throws EntityOperationForbiddenException {
        if (!settingCustomizer.canBeModified(dynamicTenantSetting)) {
            throw new EntityOperationForbiddenException("Tenant Setting Modification not allowed");
        }
    }

    private void checkModelValidity(IDynamicTenantSettingCustomizer settingCustomizer,
                                    DynamicTenantSetting dynamicTenantSetting) throws EntityInvalidException {
        if (!settingCustomizer.isValid(dynamicTenantSetting)) {
            throw new EntityInvalidException(String.format("Invalid Tenant Setting: %s", dynamicTenantSetting));
        }
    }
}
