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
package fr.cnes.regards.modules.feature.service;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.modules.model.client.IModelAttrAssocClient;
import fr.cnes.regards.modules.model.client.IModelClient;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.domain.ModelAttrAssoc;
import fr.cnes.regards.modules.model.service.validation.AbstractCacheableModelFinder;
import fr.cnes.regards.modules.model.service.validation.IModelFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Marc SORDI
 */
@Service
public class FeatureModelFinder extends AbstractCacheableModelFinder implements IModelFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureModelFinder.class);

    @Autowired
    private IModelAttrAssocClient modelAttrAssocClient;

    @Autowired
    private IModelClient modelClient;

    @Override
    protected List<ModelAttrAssoc> loadAttributesByModel(String modelName) {
        try {
            FeignSecurityManager.asSystem();
            ResponseEntity<List<EntityModel<Model>>> modelResponse = modelClient.getModels(null);

            // if the model doesn't exists we return null
            if ((modelResponse == null) || !modelResponse.getBody()
                                                         .stream()
                                                         .anyMatch(model -> model.getContent()
                                                                                 .getName()
                                                                                 .equals(modelName))) {
                return null; // NOSONAR
            }
            ResponseEntity<List<EntityModel<ModelAttrAssoc>>> response = modelAttrAssocClient.getModelAttrAssocs(
                modelName);
            List<ModelAttrAssoc> attModelAssocs = new ArrayList<>();
            if (response != null) {
                attModelAssocs = HateoasUtils.unwrapCollection(response.getBody());
            }
            return attModelAssocs;
        } catch (HttpServerErrorException | HttpClientErrorException e) {
            LOGGER.error("Error while loading the Attributes for the model {}", modelName, e);
            return Collections.emptyList();
        } finally {
            FeignSecurityManager.reset();
        }
    }
}
