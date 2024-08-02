/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.tenant.settings.dao;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

/**
 * Warning: Do not use this repository directly because encryption/decryption of settings is handled in
 * DynamicTenantSettingRepositoryService. Use this service instead.
 */
@Repository
public interface IDynamicTenantSettingRepository extends JpaRepository<DynamicTenantSetting, Long> {

    Optional<DynamicTenantSetting> findByName(String name);

    Set<DynamicTenantSetting> findAllByNameIn(Set<String> nameList);

}
