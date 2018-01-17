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

import java.util.List;
import java.util.Optional;

import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;

/**
 * {@link AcquisitionProcessingChain} repository
 *
 * @author Christophe Mertz
 * @author Marc Sordi
 */
@Repository
public interface IAcquisitionProcessingChainRepository extends JpaRepository<AcquisitionProcessingChain, Long> {

    /**
     * Find all active and not running processing chain for a specified mode
     * @param mode chain processing mode
     * @return all chains
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    List<AcquisitionProcessingChain> findByActiveTrueAndRunningFalseAndByMode(AcquisitionProcessingChainMode mode);

    /**
     * @return all automatic chains that might be started
     */
    default List<AcquisitionProcessingChain> findAllBootableAutomaticChains() {
        return findByActiveTrueAndRunningFalseAndByMode(AcquisitionProcessingChainMode.AUTO);
    }

    @Query("select chain.validationPluginConf from AcquisitionProcessingChain chain,PluginConfiguration conf where chain.id = ?1 and chain.validationPluginConf.id = conf.id")
    Optional<PluginConfiguration> findOneValidationPlugin(Long chainId);

    @Query("select chain.productPluginConf from AcquisitionProcessingChain chain,PluginConfiguration conf where chain.id = ?1 and chain.productPluginConf.id = conf.id")
    Optional<PluginConfiguration> findOneProductPlugin(Long chainId);

    @Query("select chain.generateSipPluginConf from AcquisitionProcessingChain chain,PluginConfiguration conf where chain.id = ?1 and chain.generateSipPluginConf.id = conf.id")
    Optional<PluginConfiguration> findOneGenerateSipPlugin(Long chainId);

    @Query("select chain.postProcessSipPluginConf from AcquisitionProcessingChain chain,PluginConfiguration conf where chain.id = ?1 and chain.postProcessSipPluginConf.id = conf.id")
    Optional<PluginConfiguration> findOnePostProcessSipPlugin(Long chainId);
}
