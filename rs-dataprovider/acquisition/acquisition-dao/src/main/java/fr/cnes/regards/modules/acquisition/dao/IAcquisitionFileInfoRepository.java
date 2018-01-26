/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;

/**
 * {@link AcquisitionFileInfo} repository
 *
 * @author Marc Sordi
 */
@Repository
public interface IAcquisitionFileInfoRepository extends JpaRepository<AcquisitionFileInfo, Long> {

    @Query("select fileInfo.scanPlugin from AcquisitionFileInfo fileInfo,PluginConfiguration conf where fileInfo.id = ?1 and fileInfo.scanPlugin.id = conf.id")
    Optional<PluginConfiguration> findOneScanPlugin(Long fileInfoId);
}
