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
import fr.cnes.regards.framework.modules.tenant.settings.dao.IDynamicTenantSettingRepository;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RegardsTransactional
public class DynamicTenantSettingService implements IDynamicTenantSettingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicTenantSettingService.class);

    private final List<IDynamicTenantSettingCustomizer> dynamicTenantSettingCustomizerList;

    private final IDynamicTenantSettingRepository dynamicTenantSettingRepository;

    public DynamicTenantSettingService(List<IDynamicTenantSettingCustomizer> dynamicTenantSettingCustomizerList,
                                       IDynamicTenantSettingRepository dynamicTenantSettingRepository) {
        this.dynamicTenantSettingCustomizerList = dynamicTenantSettingCustomizerList;
        this.dynamicTenantSettingRepository = dynamicTenantSettingRepository;
    }

    @Override
    public DynamicTenantSetting create(DynamicTenantSetting dynamicTenantSetting)
        throws EntityOperationForbiddenException, EntityInvalidException, EntityNotFoundException {
        dynamicTenantSetting.setId(null);
        IDynamicTenantSettingCustomizer customizer = getCustomizer(dynamicTenantSetting, false);
        DynamicTenantSetting savedDynamicTenantSetting = dynamicTenantSettingRepository.save(dynamicTenantSetting);
        customizer.doRightNow(dynamicTenantSetting);
        LOGGER.info("Created Tenant Setting {}", savedDynamicTenantSetting);
        return savedDynamicTenantSetting;
    }

    @Override
    public Optional<DynamicTenantSetting> read(String name) {
        return dynamicTenantSettingRepository.findByName(name);
    }

    @Override
    public Set<DynamicTenantSetting> readAll(Set<String> nameList) {
        return dynamicTenantSettingRepository.findAllByNameIn(nameList);
    }

    @Override
    public Set<DynamicTenantSetting> readAll() {
        return new HashSet<>(dynamicTenantSettingRepository.findAll());
    }

    @Override
    public <T> DynamicTenantSetting update(String name, T value)
        throws EntityNotFoundException, EntityOperationForbiddenException, EntityInvalidException {
        DynamicTenantSetting dynamicTenantSetting = getDynamicTenantSetting(name);
        if (!Objects.equals(value, dynamicTenantSetting.getValue())) {
            dynamicTenantSetting.setValue(value);
            IDynamicTenantSettingCustomizer customizer = getCustomizer(dynamicTenantSetting, true);
            dynamicTenantSetting = dynamicTenantSettingRepository.save(dynamicTenantSetting);
            customizer.doRightNow(dynamicTenantSetting);
            LOGGER.info("Updated Tenant Setting {}", dynamicTenantSetting);
        }
        return dynamicTenantSetting;
    }

    @Override
    public void delete(String name) throws EntityNotFoundException {
        DynamicTenantSetting dynamicTenantSetting = getDynamicTenantSetting(name);
        dynamicTenantSettingRepository.delete(dynamicTenantSetting);
        LOGGER.info("Deleted Tenant Setting {}", name);
    }

    @Override
    public DynamicTenantSetting reset(String name)
        throws EntityNotFoundException, EntityInvalidException, EntityOperationForbiddenException {
        DynamicTenantSetting dynamicTenantSetting = getDynamicTenantSetting(name);
        if (!dynamicTenantSetting.getDefaultValue().equals(dynamicTenantSetting.getValue())) {
            dynamicTenantSetting.setValue(dynamicTenantSetting.getDefaultValue());
            IDynamicTenantSettingCustomizer customizer = getCustomizer(dynamicTenantSetting, true);
            dynamicTenantSetting = dynamicTenantSettingRepository.save(dynamicTenantSetting);
            customizer.doRightNow(dynamicTenantSetting);
        }
        LOGGER.info("Reset Tenant Setting {}", dynamicTenantSetting);
        return dynamicTenantSetting;
    }

    @Override
    public boolean canUpdate(String name) {
        try {
            // we are using getCustomizer exception so we do not have to duplicate research logic
            DynamicTenantSetting dynamicTenantSetting = getDynamicTenantSetting(name);
            getCustomizer(dynamicTenantSetting, true);
            return true;
        } catch (EntityInvalidException e) {
            // even if value is invalid(which is highly improbable at this point because we are looking for DB value),
            // we can update the setting
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

    private DynamicTenantSetting getDynamicTenantSetting(String name) throws EntityNotFoundException {
        return read(name).orElseThrow(() -> new EntityNotFoundException(name, DynamicTenantSetting.class));
    }

    private IDynamicTenantSettingCustomizer getCustomizer(DynamicTenantSetting dynamicTenantSetting,
                                                          boolean checkModification)
        throws EntityInvalidException, EntityOperationForbiddenException, EntityNotFoundException {

        IDynamicTenantSettingCustomizer settingCustomizer = dynamicTenantSettingCustomizerList.stream()
                                                                                              .filter(customizer -> customizer.appliesTo(
                                                                                                  dynamicTenantSetting))
                                                                                              .findAny()
                                                                                              .orElseThrow(() -> new EntityNotFoundException(
                                                                                                  dynamicTenantSetting.getName(),
                                                                                                  IDynamicTenantSettingCustomizer.class));

        if (checkModification && !settingCustomizer.canBeModified(dynamicTenantSetting)) {
            throw new EntityOperationForbiddenException("Tenant Setting Modification not allowed");
        }

        if (!settingCustomizer.isValid(dynamicTenantSetting)) {
            throw new EntityInvalidException(String.format("Invalid Tenant Setting: %s", dynamicTenantSetting));
        }

        return settingCustomizer;
    }

}
