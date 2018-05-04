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
package fr.cnes.regards.modules.acquisition.rest;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionFileService;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionProcessingService;

/**
 * {@link AcquisitionFile} REST API testing
 *
 * @author Marc Sordi
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=acquisition_it" })
@RegardsTransactional
public class AcquisitionFileControllerIT extends AbstractRegardsTransactionalIT {

    @Autowired
    private IAcquisitionFileService fileService;

    @Autowired
    private IAcquisitionProcessingService processingService;

    @Before
    public void init() {
        AcquisitionProcessingChain processingChain= AcquisitionTestUtils.getNewChain("Test");
        processingService.createChain(processingChain);

        fileService.save(file)
    }

    @Test
    public void searchForFilesTest() throws ModuleException {
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultGet(AcquisitionFileController.TYPE_PATH, requestBuilderCustomizer, "Should retrieve files");

        // requestBuilderCustomizer.customizeRequestParam().param("sipState", "NOT_SCHEDULED", "QUEUED");
        // performDefaultGet(ProductController.TYPE_PATH, requestBuilderCustomizer, "Should retrieve products");
    }
}
