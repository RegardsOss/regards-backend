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

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.modules.tenant.settings.dao.IDynamicTenantSettingRepository;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RegardsTransactional
public class DynamicTenantSettingService implements IDynamicTenantSettingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicTenantSettingService.class);

    private final List<IDynamicTenantSettingCustomizer> dynamicTenantSettingCustomizerList;

    private final IDynamicTenantSettingRepository dynamicTenantSettingRepository;

    public DynamicTenantSettingService(List<IDynamicTenantSettingCustomizer> dynamicTenantSettingCustomizerList,
                                       IDynamicTenantSettingRepository dynamicTenantSettingRepository
    ) {
        this.dynamicTenantSettingCustomizerList = dynamicTenantSettingCustomizerList;
        this.dynamicTenantSettingRepository = dynamicTenantSettingRepository;
    }

    @Override
    public DynamicTenantSetting create(DynamicTenantSetting dynamicTenantSetting) throws EntityOperationForbiddenException, EntityInvalidException, EntityNotFoundException {
        dynamicTenantSetting.setId(null);
        IDynamicTenantSettingCustomizer customizer = getCustomizer(dynamicTenantSetting);
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
    public List<DynamicTenantSetting> readAll(List<String> nameList) {
        return dynamicTenantSettingRepository.findAllByNameIn(nameList);
    }

    @Override
    public List<DynamicTenantSetting> readAll() {
        return dynamicTenantSettingRepository.findAll();
    }

    @Override
    public <T> DynamicTenantSetting update(String name, T value) throws EntityNotFoundException, EntityOperationForbiddenException, EntityInvalidException {
        DynamicTenantSetting dynamicTenantSetting = getDynamicTenantSetting(name);
        if (!Objects.equals(value, dynamicTenantSetting.getValue())) {
            dynamicTenantSetting.setValue(value);
            IDynamicTenantSettingCustomizer customizer = getCustomizer(dynamicTenantSetting);
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
    public void reset(String name) throws EntityNotFoundException, EntityInvalidException, EntityOperationForbiddenException {
        DynamicTenantSetting dynamicTenantSetting = getDynamicTenantSetting(name);
        if (!dynamicTenantSetting.getDefaultValue().equals(dynamicTenantSetting.getValue())) {
            dynamicTenantSetting.setValue(dynamicTenantSetting.getDefaultValue());
            IDynamicTenantSettingCustomizer customizer = getCustomizer(dynamicTenantSetting);
            dynamicTenantSettingRepository.save(dynamicTenantSetting);
            customizer.doRightNow(dynamicTenantSetting);
        }
        LOGGER.info("Reset Tenant Setting {}", dynamicTenantSetting);
    }

    private DynamicTenantSetting getDynamicTenantSetting(String name) throws EntityNotFoundException {
        return read(name).orElseThrow(() -> new EntityNotFoundException(name, DynamicTenantSetting.class));
    }

    private IDynamicTenantSettingCustomizer getCustomizer(DynamicTenantSetting dynamicTenantSetting) throws EntityInvalidException, EntityOperationForbiddenException, EntityNotFoundException {

        IDynamicTenantSettingCustomizer settingCustomizer = dynamicTenantSettingCustomizerList
                .stream()
                .filter(customizer -> customizer.appliesTo(dynamicTenantSetting))
                .findAny()
                .orElseThrow(() -> new EntityNotFoundException(dynamicTenantSetting.getName(), IDynamicTenantSettingCustomizer.class));

        if (!settingCustomizer.canBeModified(dynamicTenantSetting)) {
            throw new EntityOperationForbiddenException("Tenant Setting Modification not allowed");
        }

        if (!settingCustomizer.isValid(dynamicTenantSetting)) {
            throw new EntityInvalidException("Invalid Tenant Setting");
        }

        return settingCustomizer;
    }

}
