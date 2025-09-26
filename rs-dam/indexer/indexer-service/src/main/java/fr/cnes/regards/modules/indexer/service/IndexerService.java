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
package fr.cnes.regards.modules.indexer.service;

import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.indexer.dao.BulkSaveResult;
import fr.cnes.regards.modules.indexer.domain.IIndexable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;

/**
 * To be removed, obviously this service is no more used by microservices, only by tests
 *
 * @deprecated
 */
@Deprecated
@Service
public class IndexerService implements IIndexerService {

    @Autowired
    private EsRepositoryFacade esRepositoryFacade;

    @Override
    public boolean createIndex(String pIndex) {
        if (!esRepositoryFacade.indexExists(pIndex)) {
            boolean created = esRepositoryFacade.createIndex(pIndex);
            if (created) {
                String[] types = Arrays.stream(EntityType.values())
                                       .map(EntityType::toString)
                                       .toArray(length -> new String[length]);
            }
            return created;
        }
        return true;
    }

    @Override
    public boolean deleteIndex(String pIndex) {
        if (esRepositoryFacade.indexExists(pIndex)) {
            return esRepositoryFacade.deleteIndexOrAlias(pIndex);
        }
        return true;
    }

    @Override
    public boolean indexExists(String pIndex) {
        return esRepositoryFacade.indexExists(pIndex);
    }

    @Override
    public boolean saveEntity(String pIndex, IIndexable pEntity) {
        return esRepositoryFacade.saveToIndexOrAlias(pIndex, pEntity);
    }

    @Override
    public void refresh(String pIndex) {
        esRepositoryFacade.refreshIndex(pIndex);
    }

    @Override
    public BulkSaveResult saveBulkEntities(String pIndex, Collection<? extends IIndexable> pEntities) {
        return esRepositoryFacade.saveBulkToIndexOrAlias(pIndex, pEntities);
    }

}