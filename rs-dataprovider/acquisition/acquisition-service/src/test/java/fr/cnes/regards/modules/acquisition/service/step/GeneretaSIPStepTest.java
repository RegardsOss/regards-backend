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

package fr.cnes.regards.modules.acquisition.service.step;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.gson.Gson;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.acquisition.builder.AcquisitionFileBuilder;
import fr.cnes.regards.modules.acquisition.builder.ProductBuilder;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileStatus;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductStatus;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.MetaProductDto;
import fr.cnes.regards.modules.acquisition.plugins.IGenerateSIPPlugin;
import fr.cnes.regards.modules.acquisition.service.conf.ChainGenerationServiceConfiguration;
import fr.cnes.regards.modules.acquisition.service.job.AcquisitionProcess;
import fr.cnes.regards.modules.acquisition.service.plugins.TestGenerateSipPlugin;

/**
 * @author Christophe Mertz
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { ChainGenerationServiceConfiguration.class })
@ActiveProfiles({ "test", "disableDataProviderTask" })
@DirtiesContext
public class GeneretaSIPStepTest extends AbstractAcquisitionIT {

    private static final String SESSION_ID = "session-identifier-999";

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IGenerateSipStep generateSIPStepImpl;

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Test
    public void proceedStep() throws ModuleException {
        String metaProductJson = new Gson().toJson(MetaProductDto.fromMetaProduct(metaProduct));

        // Configure a plugin IGenerateSIPPlugin
        chain.setGenerateSIPPluginConf(pluginService.getPluginConfiguration("TestGenerateSipPlugin",
                                                                            IGenerateSIPPlugin.class));
        chain.addGenerateSIPParameter(TestGenerateSipPlugin.META_PRODUCT_PARAM, metaProductJson);
        chain.addGenerateSIPParameter(TestGenerateSipPlugin.SESSION_PARAM, SESSION_ID);

        // Create a Product
        Product product = productService.save(ProductBuilder.build(FIRST_PRODUCT).withStatus(ProductStatus.COMPLETED)
                .withMetaProduct(metaProduct).withSession(SESSION_ID).get());

        // Add AcquisitionFile to the Product
        product.addAcquisitionFile(acquisitionFileService.save(AcquisitionFileBuilder.build("file-1")
                .withStatus(AcquisitionFileStatus.VALID.toString()).withMetaFile(metaFileMandatory).get()));
        product.addAcquisitionFile(acquisitionFileService.save(AcquisitionFileBuilder.build("file-2")
                .withStatus(AcquisitionFileStatus.VALID.toString()).withMetaFile(metaFileMandatory).get()));
        productService.save(product);

        AcquisitionProcess process = new AcquisitionProcess(chain, product);
        IStep generateSIPStep = generateSIPStepImpl;
        generateSIPStep.setProcess(process);
        beanFactory.autowireBean(generateSIPStep);
        process.setCurrentStep(generateSIPStep);

        process.run();

        Product productRead = productService.retrieve(product.getProductName());
        Assert.assertNotNull(productRead.getSip());
    }

    @Test(expected = RuntimeException.class)
    public void proceedStepWithoutSipPLugin() throws ModuleException {
        // Create a Product
        Product product = productService.save(ProductBuilder.build(FIRST_PRODUCT).withStatus(ProductStatus.COMPLETED)
                .withMetaProduct(metaProduct).withSession(SESSION_ID).get());

        // Add to AcquisitionFile to the Product
        product.addAcquisitionFile(acquisitionFileService.save(AcquisitionFileBuilder.build("file-1")
                .withStatus(AcquisitionFileStatus.VALID.toString()).withMetaFile(metaFileMandatory).get()));
        product.addAcquisitionFile(acquisitionFileService.save(AcquisitionFileBuilder.build("file-2")
                .withStatus(AcquisitionFileStatus.VALID.toString()).withMetaFile(metaFileMandatory).get()));
        productService.save(product);

        AcquisitionProcess process = new AcquisitionProcess(chain, product);
        IStep generateSIPStep = generateSIPStepImpl;
        generateSIPStep.setProcess(process);
        beanFactory.autowireBean(generateSIPStep);
        process.setCurrentStep(generateSIPStep);

        process.run();

        Assert.fail();
    }

}
