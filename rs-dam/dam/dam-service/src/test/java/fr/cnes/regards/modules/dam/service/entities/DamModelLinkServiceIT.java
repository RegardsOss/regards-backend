/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.dam.service.entities;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.dam.dao.entities.IDatasetRepository;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.model.dao.IModelRepository;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.service.IModelService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 * @author Léo Mieulet
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=entities",
                                   "regards.dam.post.aip.entities.to.storage=false" },
                    locations = "classpath:es.properties")
@MultitenantTransactional
public class DamModelLinkServiceIT extends AbstractMultitenantServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(DamModelLinkServiceIT.class);

    private static final String datasetModelFileName = "datasetModel.xml";

    @Autowired
    private IModelService modelService;

    @Autowired
    private IDatasetRepository datasetRepository;

    @Autowired
    private IModelRepository modelRepository;

    @Autowired
    private IDatasetService datasetService;

    @Autowired
    private DamModelLinkService damModelLinkService;

    @Before
    public void init() throws ModuleException {
        datasetRepository.deleteAll();
        modelRepository.deleteAll();
    }

    @Test
    public void testPerformanceIsAttributeDeletableWhenManyDatasets() throws ModuleException {
        // given
        Model datasetModel = importModel(datasetModelFileName);
        for (int datasetId = 0; datasetId < 1000; datasetId++) {
            createDataset(datasetModel, datasetId);
        }
        saveDatasetsIntoDB();
        long startTime = System.currentTimeMillis();
        // when
        for (int i = 0; i < 100; i++) {
            damModelLinkService.isAttributeDeletable(Sets.newSet(datasetModel.getName()));
        }
        // then
        long duration = System.currentTimeMillis() - startTime;
        LOGGER.info("Requests 100 attributes isAttributeDeletable() took {} ms", duration);
        Assert.assertTrue("computing isAttributeDeletable() must stay fast", duration < 1000);
    }

    private void saveDatasetsIntoDB() {
        entityManager.flush();
        entityManager.clear();
    }

    private void createDataset(Model datasetModel, int datasetId) throws ModuleException {
        Dataset dataset = new Dataset(datasetModel, "PROJECT", "DS_" + datasetId, "Dataset " + datasetId);
        Errors validationErrors = new MapBindingResult(new HashMap<>(), Dataset.class.getName());
        datasetService.createDataset(dataset, validationErrors);
        Assert.assertFalse("dataset creation is fine", validationErrors.hasErrors());
    }

    /**
     * Import model definition file from resources directory
     *
     * @param filename filename
     * @throws ModuleException if error occurs
     */
    private Model importModel(String filename) throws ModuleException {
        try {
            final InputStream input = Files.newInputStream(Paths.get("src", "test", "resources", filename));
            return modelService.importModel(input);
        } catch (IOException e) {
            e.printStackTrace();
            String errorMessage = "Cannot import " + filename;
            throw new AssertionError(errorMessage);
        }
    }
}
