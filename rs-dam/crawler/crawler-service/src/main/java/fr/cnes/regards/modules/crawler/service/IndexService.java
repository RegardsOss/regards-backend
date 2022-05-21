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
package fr.cnes.regards.modules.crawler.service;

import fr.cnes.regards.modules.dam.service.settings.IDamSettingsService;
import fr.cnes.regards.modules.indexer.dao.CreateIndexConfiguration;
import fr.cnes.regards.modules.indexer.dao.EsRepository;
import fr.cnes.regards.modules.indexer.service.IMappingService;
import fr.cnes.regards.modules.model.domain.ModelAttrAssoc;
import fr.cnes.regards.modules.model.service.IModelAttrAssocService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Thibaud Michaudel
 **/
@Service
public class IndexService {

    @Autowired
    private EsRepository esRepos;

    @Autowired
    private IMappingService esMappingService;

    @Autowired
    private IModelAttrAssocService modelAttrAssocService;

    @Autowired
    private IDamSettingsService damSettingsService;

    public void configureMappings(String tenant, String modelName) {
        List<ModelAttrAssoc> modelAttributes = modelAttrAssocService.getModelAttrAssocs(modelName);
        esMappingService.configureMappings(tenant, modelAttributes);
    }

    /**
     * Create index if it doesn't exist
     *
     * @param tenant concerned tenant
     * @return true if a creation has been done
     */
    public boolean createIndexIfNeeded(String tenant) {
        if (esRepos.indexExists(tenant)) {
            return false;
        }
        CreateIndexConfiguration configuration = new CreateIndexConfiguration(damSettingsService.getIndexNumberOfShards(),
                                                                              damSettingsService.getIndexNumberOfReplicas());
        return esRepos.createIndex(tenant, configuration);
    }

    /**
     * Delete index if it exists
     *
     * @param tenant concerned tenant
     * @return true if a deletion has been done
     */
    public boolean deleteIndex(String tenant) {
        if (!esRepos.indexExists(tenant)) {
            return false;
        }
        return esRepos.deleteIndex(tenant);
    }
}
