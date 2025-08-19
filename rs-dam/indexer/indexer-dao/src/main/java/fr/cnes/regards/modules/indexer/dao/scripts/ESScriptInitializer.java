/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.indexer.dao.scripts;

import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * This component registers Elasticsearch scripts in the cluster when the application is ready.
 * These scripts can then be used on Elasticsearch indices by referencing their IDs.
 * Useful to avoid sending entire scripts with each request.
 *
 * @author tguillou
 */
@Component
public class ESScriptInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final IEsRepository elasticSearchRepository;

    public ESScriptInitializer(IEsRepository elasticSearchRepository) {
        this.elasticSearchRepository = elasticSearchRepository;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            List<AbstractEsScript> scripts = List.of(new UpsertDataObjectEsScript(),
                                                     new UpdateGroupsAndDatasetAssociationEsScript());
            for (AbstractEsScript script : scripts) {
                elasticSearchRepository.registerScript(script.getScriptId(), script.getScriptContent());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to register Elasticsearch script", e);
        }
    }

}
