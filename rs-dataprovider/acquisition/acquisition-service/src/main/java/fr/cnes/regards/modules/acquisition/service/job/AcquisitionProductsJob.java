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

package fr.cnes.regards.modules.acquisition.service.job;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductStatus;
import fr.cnes.regards.modules.acquisition.domain.job.ChainGenerationJobParameter;
import fr.cnes.regards.modules.acquisition.domain.job.ProductJobParameter;
import fr.cnes.regards.modules.acquisition.service.IChainGenerationService;
import fr.cnes.regards.modules.acquisition.service.IProductService;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionRuntimeException;
import fr.cnes.regards.modules.acquisition.service.step.AcquisitionCheckStep;
import fr.cnes.regards.modules.acquisition.service.step.AcquisitionScanStep;
import fr.cnes.regards.modules.acquisition.service.step.IStep;

/**
 * This class runs a set of step :<br>
 * <li>a step {@link AcquisitionScanStep} to scan and identify the {@link AcquisitionFile} to acquired
 * <li>a step {@link AcquisitionCheckStep} to check the {@link AcquisitionFile} and to determines the {@link Product} associated<br>
 * And for each scanned {@link Product} not already send to Ingest microservice, and with his status equals to {@link ProductStatus#COMPLETED} or {@link ProductStatus#FINISHED},
 * a new {@link JobInfo} of class {@link AcquisitionGenerateSIPJob} is create and queued. 
 *  
 * @author Christophe Mertz
 *
 */
public class AcquisitionProductsJob extends AbstractJob<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionProductsJob.class);

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    /**
     * {@link Product} srvice
     */
    @Autowired
    private IProductService productService;

    /**
     * {@link JobInfo} service
     */
    @Autowired
    private IJobInfoService jobInfoService;

    /**
     * {@link ChainGeneration} service
     */
    @Autowired
    private IChainGenerationService chainGenerationService;

    /**
     * Resolver to retrieve authentication information
     */
    @Autowired
    private IAuthenticationResolver authResolver;

    /**
     * The current {@link ChainGeneration}
     */
    private ChainGeneration chainGeneration;

    @Override
    public void run() {
        LOGGER.info("[{}] Start acquisition job for the chain <{}>", chainGeneration.getSession(),
                    chainGeneration.getLabel());

        // The MetaProduct is required
        if (chainGeneration.getMetaProduct() == null) {
            throw new AcquisitionRuntimeException(
                    "The required MetaProduct is missing for the ChainGeneration <" + chainGeneration.getLabel() + ">");
        }

        AcquisitionProcess process = new AcquisitionProcess(chainGeneration);

        // IAcquisitionScanStep is the first step
        IStep scanStep = new AcquisitionScanStep();
        scanStep.setProcess(process);
        beanFactory.autowireBean(scanStep);
        process.setCurrentStep(scanStep);

        // IAcquisitionCheckStep is second step
        IStep checkStep = null;
        if (chainGeneration.getCheckAcquisitionPluginConf() != null) {
            checkStep = new AcquisitionCheckStep();
            checkStep.setProcess(process);
            beanFactory.autowireBean(checkStep);
            scanStep.setNextStep(checkStep);
        }

        process.run();

        // for each Product, create and queued a Job to generate SIP and send it to Ingest microservice
        final int n = submitProducts();

        // the ChainGeneration is not running, it is available for a new scan
        chainGeneration.setRunning(false);
        try {
            chainGenerationService.createOrUpdate(chainGeneration);
        } catch (ModuleException e) {
            LOGGER.error("[{}] Error when try to save the chain {}", chainGeneration.getSession(),
                         chainGeneration.getLabel());
            LOGGER.error(e.getMessage(), e);
        }

        LOGGER.info("[{}] {} AcquisitionGenerateSIPJob queued", chainGeneration.getSession(), n);

        LOGGER.info("[{}] End  acquisition job for the chain <{}>", chainGeneration.getSession(),
                    chainGeneration.getLabel());
    }

    /**
     * Create and queued a {@link JobInfo} of class {@link AcquisitionGenerateSIPJob} for each {@link Product} not already send to Ingest microservice,<br>
     * and with his status equals to {@link ProductStatus#COMPLETED} or {@link ProductStatus#FINISHED}.
     * @return the number of {@link JobInfo} create and queued
     */
    private int submitProducts() {
        List<Product> products = new ArrayList<>();
        products.addAll(productService.findBySendedAndStatusIn(Boolean.FALSE, ProductStatus.COMPLETED,
                                                               ProductStatus.FINISHED));
        int nbJobQueued = 0;
        for (Product apr : products) {
            if (createJob(apr)) {
                nbJobQueued++;
            } else {
                LOGGER.error("error :{}", apr.getProductName());
            }
        }

        return nbJobQueued;
    }

    /**
     * Create and queued a {@link JobInfo} of class {@link AcquisitionGenerateSIPJob} for a {@link Product}.
     * @param product the {@link Product} to process
     * @return true if the {@link JobInfo} is create and queued
     */
    private boolean createJob(Product product) {
        // Create a ScanJob
        JobInfo acquisition = new JobInfo();
        acquisition.setParameters(new ChainGenerationJobParameter(chainGeneration),
                                  new ProductJobParameter(product.getProductName()));
        acquisition.setClassName(AcquisitionGenerateSIPJob.class.getName());
        acquisition.setOwner(authResolver.getUser());
        acquisition.setPriority(50); //TODO CMZ priority ?

        acquisition = jobInfoService.createAsQueued(acquisition);

        return acquisition != null;
    }

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        if (parameters.isEmpty()) {
            throw new JobParameterMissingException("No parameter provided");
        }
        if (parameters.size() != 1) {
            throw new JobParameterInvalidException("Only one parameter is expected.");
        }
        JobParameter param = parameters.values().iterator().next();
        if (!ChainGenerationJobParameter.isCompatible(param)) {
            throw new JobParameterInvalidException(
                    "Please use ChainGenerationJobParameter in place of JobParameter (this "
                            + "class is here to facilitate your life so please use it.");
        }

        chainGeneration = param.getValue();
    }
}
