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
package fr.cnes.regards.modules.crawler.service.service;

import com.google.common.base.Strings;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.dam.service.settings.IDamSettingsService;
import fr.cnes.regards.modules.indexer.dao.CreateIndexConfiguration;
import fr.cnes.regards.modules.indexer.dao.EsRepository;
import fr.cnes.regards.modules.indexer.service.IMappingService;
import fr.cnes.regards.modules.indexer.service.IndexAliasResolver;
import fr.cnes.regards.modules.indexer.service.IndexAliasService;
import fr.cnes.regards.modules.model.domain.ModelAttrAssoc;
import fr.cnes.regards.modules.model.service.IModelAttrAssocService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Centralizes operations related to the lifecycle  of Elasticsearch indices and aliases
 *
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

    @Autowired
    private IndexAliasService indexAliasService;

    @Autowired
    private IndexAliasResolver indexAliasResolver;

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexService.class);

    public void configureMappings(String tenant, String modelName) {
        List<ModelAttrAssoc> modelAttributes = modelAttrAssocService.getModelAttrAssocs(modelName);
        esMappingService.configureMappings(tenant, modelAttributes);
    }

    /**
     * Creates a new Elasticsearch index for the given tenant if it does not already exist,
     * and ensures that the corresponding alias is created or updated to point to it.
     *
     * <p>The index name is derived directly from the tenant identifier.
     * If the index already exists, no action is performed and the method returns {@code false}.
     * If the index is created, the alias is also created or updated to ensure
     * that all queries go through the alias rather than the raw index.</p>
     *
     * @param tenant the tenant identifier used as the index name
     * @return {@code true} if the index was created, {@code false} if it already existed
     */
    public boolean createIndexAndAliasIfNeeded(String tenant) {
        if (esRepos.indexExists(tenant)) {
            return false;
        }
        CreateIndexConfiguration configuration = new CreateIndexConfiguration(damSettingsService.getIndexNumberOfShards(),
                                                                              damSettingsService.getIndexNumberOfReplicas());
        boolean created = esRepos.createIndex(tenant, configuration);
        createOrUpdateAlias(tenant);
        return created;
    }

    /**
     * Ensures that, for a given tenant, the ES alias exists and correctly points to the expected index.
     *
     * <p>If the alias does not exist, it is created and persisted in the database, targeting
     * the tenant's current index (with the same name as the tenant by default).
     * If the alias already exists, its target index is validated against the one recorded in the database:
     * <ul>
     *   <li>If the alias points to the correct index, nothing is done.</li>
     *   <li>If the alias points to an outdated or incorrect index, it is switched to the correct one.</li>
     *   <li>If no current index is resolved from the database, an {@link IllegalStateException} is thrown,
     *       as this indicates an inconsistency between Elasticsearch and the database.</li>
     * </ul>
     *
     * @param tenant the tenant identifier whose alias must be created or updated
     * @throws IllegalStateException if the alias exists but no current index is resolved for the tenant
     */
    public void createOrUpdateAlias(String tenant) {
        String aliasName = indexAliasResolver.resolveAliasName(tenant);

        //If alias does not exist, it is created here
        if (!esRepos.aliasExists(aliasName)) {
            if (esRepos.createAlias(tenant, aliasName)) {
                indexAliasService.saveOrUpdate(aliasName, tenant);
                LOGGER.info("Alias [{}] created on index [{}]", aliasName, tenant);
            } else {
                LOGGER.error("Alias [{}] creation not acknowledged on index [{}]", aliasName, tenant);
                throw new RsRuntimeException("Alias " + aliasName + " could not be created on index " + tenant);

            }
            return;
        }

        //We control if the alias exists in the DB. If, for some reason, it is absent from the DB, we add it, as
        // ElasticSearch and the DB must be synchronized all the time.
        if (indexAliasService.getByAlias(aliasName) == null) {
            indexAliasService.saveOrUpdate(aliasName, tenant);
            return;
        }

        //If alias is already present, we need to check its target index and maybe update it
        String targetIndex = indexAliasResolver.resolveCurrentIndex(tenant);

        //We should have a current index in the DB if alias already exists. If not, there is a problem
        if (Strings.isNullOrEmpty(targetIndex)) {
            throw new IllegalStateException(String.format(
                "Cannot update alias [%s]: no current index resolved for tenant [%s]",
                aliasName,
                tenant));
        }

        //The correct index mapped by the alias is the one in the entity. If the alias already exists but with a bad
        // mapped index, we need to change its index
        String aliasPointsTo = esRepos.getSingleIndexPointedByAlias(aliasName);
        if (!targetIndex.equals(aliasPointsTo)) {
            boolean switched = esRepos.switchAlias(aliasPointsTo, targetIndex, aliasName);
            if (switched) {
                LOGGER.info("Alias [{}] switched from [{}] to [{}]", aliasName, aliasPointsTo, targetIndex);
            } else {
                LOGGER.error("Alias [{}] switch not acknowledged ({} to {})", aliasName, aliasPointsTo, targetIndex);
            }
        }
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
