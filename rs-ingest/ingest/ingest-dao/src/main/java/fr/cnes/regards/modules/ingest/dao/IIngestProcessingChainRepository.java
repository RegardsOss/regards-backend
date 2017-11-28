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
package fr.cnes.regards.modules.ingest.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;

/**
 * {@link IngestProcessingChain} repository
 *
 * @author Marc Sordi
 *
 */
public interface IIngestProcessingChainRepository
        extends JpaRepository<IngestProcessingChain, Long>, JpaSpecificationExecutor<IngestProcessingChain> {

    /**
     * Retrieve chain with specified name
     * @param name processing chain name
     * @return {@link IngestProcessingChain}
     */
    Optional<IngestProcessingChain> findOneByName(String name);

    Long countByName(String name);

    default boolean exists(String name) {
        return countByName(name) == 1;
    }

    @Query("select chain.preProcessingPlugin from IngestProcessingChain chain,PluginConfiguration conf where chain.name = ?1 and chain.preProcessingPlugin.id = conf.id")
    Optional<PluginConfiguration> findOnePreProcessingPluginByName(String name);

    @Query("select chain.validationPlugin from IngestProcessingChain chain,PluginConfiguration conf where chain.name = ?1 and chain.validationPlugin.id = conf.id")
    Optional<PluginConfiguration> findOneValidationPluginByName(String name);

    @Query("select chain.generationPlugin from IngestProcessingChain chain,PluginConfiguration conf where chain.name = ?1 and chain.generationPlugin.id = conf.id")
    Optional<PluginConfiguration> findOneGenerationPluginByName(String name);

    @Query("select chain.tagPlugin from IngestProcessingChain chain,PluginConfiguration conf where chain.name = ?1 and chain.tagPlugin.id = conf.id")
    Optional<PluginConfiguration> findOneTagPluginByName(String name);

    @Query("select chain.postProcessingPlugin from IngestProcessingChain chain,PluginConfiguration conf where chain.name = ?1 and chain.postProcessingPlugin.id = conf.id")
    Optional<PluginConfiguration> findOnePostProcessingPluginByName(String name);
}
