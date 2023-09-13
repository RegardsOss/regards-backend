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
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.tenant.settings.dao.IDynamicTenantSettingRepository;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.encryption.SensitiveDynamicSettingConverter;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service that communicates with {@link IDynamicTenantSettingRepository}. Handles encrypting and decrypting of
 * sensitive settings.
 *
 * @author Iliana Ghazali
 **/
public class DynamicTenantSettingRepositoryService {

    private final IDynamicTenantSettingRepository dynamicTenantSettingRepository;

    private final SensitiveDynamicSettingConverter sensitiveDynamicSettingConverter;

    public DynamicTenantSettingRepositoryService(IDynamicTenantSettingRepository dynamicTenantSettingRepository,
                                                 SensitiveDynamicSettingConverter sensitiveDynamicSettingConverter) {
        this.dynamicTenantSettingRepository = dynamicTenantSettingRepository;
        this.sensitiveDynamicSettingConverter = sensitiveDynamicSettingConverter;
    }

    // ------------
    // -- SEARCH --
    // ------------
    @RegardsTransactional(readOnly = true)
    public Set<DynamicTenantSetting> findAll(boolean decryptSensitiveValues) {
        return dynamicTenantSettingRepository.findAll()
                                             .stream()
                                             .map(dynamicTenantSetting -> sensitiveDynamicSettingConverter.getDynamicSettingWithSensitiveValues(
                                                 dynamicTenantSetting,
                                                 decryptSensitiveValues))
                                             .collect(Collectors.toSet());
    }

    @RegardsTransactional(readOnly = true)
    public Set<DynamicTenantSetting> findAllByNameIn(Set<String> names, boolean decryptSensitiveValues) {
        return dynamicTenantSettingRepository.findAllByNameIn(names)
                                             .stream()
                                             .map(dynamicTenantSetting -> sensitiveDynamicSettingConverter.getDynamicSettingWithSensitiveValues(
                                                 dynamicTenantSetting,
                                                 decryptSensitiveValues))
                                             .collect(Collectors.toSet());
    }

    @RegardsTransactional(readOnly = true)
    public Optional<DynamicTenantSetting> findByName(String name, boolean decryptSensitiveValues) {
        return dynamicTenantSettingRepository.findByName(name)
                                             .map(dynamicTenantSetting -> sensitiveDynamicSettingConverter.getDynamicSettingWithSensitiveValues(
                                                 dynamicTenantSetting,
                                                 decryptSensitiveValues));
    }

    @RegardsTransactional(readOnly = true)
    public DynamicTenantSetting findByNameWithExceptionOnNotFound(String name, boolean decryptSensitiveValues)
        throws EntityNotFoundException {
        return findByName(name, decryptSensitiveValues).orElseThrow(() -> new EntityNotFoundException(name,
                                                                                                      DynamicTenantSetting.class));
    }

    // ------------
    // -- UPDATE --
    // ------------

    @RegardsTransactional
    public DynamicTenantSetting save(DynamicTenantSetting dynamicTenantSetting,
                                     @Nullable DynamicTenantSetting oldDynamicTenantSetting)
        throws EntityNotFoundException {
        return dynamicTenantSettingRepository.save(sensitiveDynamicSettingConverter.encryptDynamicSettingWithSensitiveValues(
            dynamicTenantSetting,
            oldDynamicTenantSetting));
    }

    // ------------
    // -- DELETE --
    // ------------

    @RegardsTransactional
    public void delete(DynamicTenantSetting dynamicTenantSetting) {
        dynamicTenantSettingRepository.delete(dynamicTenantSetting);
    }

}
