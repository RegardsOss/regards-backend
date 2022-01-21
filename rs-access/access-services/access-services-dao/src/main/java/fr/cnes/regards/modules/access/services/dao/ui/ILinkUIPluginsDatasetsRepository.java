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
package fr.cnes.regards.modules.access.services.dao.ui;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.access.services.domain.ui.LinkUIPluginsDatasets;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration;

/**
 * JPA Respository to access {@link LinkUIPluginsDatasets} entities
 *
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 */
public interface ILinkUIPluginsDatasetsRepository extends JpaRepository<LinkUIPluginsDatasets, Long> {

    LinkUIPluginsDatasets findOneByDatasetId(String datasetId);

    List<LinkUIPluginsDatasets> findByDatasetIdIn(List<String> datasetId);

    /**
     * Retrieve all links having the given configuration in their services list
     *
     * @param pluginConfiguration
     * @return The query result wrapped in a {@link Stream}
     */
    Stream<LinkUIPluginsDatasets> findAllByServicesContaining(UIPluginConfiguration pluginConfiguration);

}
