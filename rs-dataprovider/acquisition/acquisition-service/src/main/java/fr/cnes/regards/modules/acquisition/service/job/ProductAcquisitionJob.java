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

import java.util.Map;
import java.util.Set;

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
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;
import fr.cnes.regards.framework.modules.jobs.domain.step.IProcessingStep;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductStatus;
import fr.cnes.regards.modules.acquisition.domain.job.AcquisitionProcessingChainJobParameter;
import fr.cnes.regards.modules.acquisition.domain.job.ProductJobParameter;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionProcessingChainService;
import fr.cnes.regards.modules.acquisition.service.IProductService;
import fr.cnes.regards.modules.acquisition.service.job.step.AcquisitionCheckStep;
import fr.cnes.regards.modules.acquisition.service.job.step.AcquisitionScanStep;

/**
 * This class runs a set of step :<br>
 * <li>a step {@link AcquisitionScanStep} to scan and identify the {@link AcquisitionFile} to acquired
 * <li>a step {@link AcquisitionCheckStep} to check the {@link AcquisitionFile} and to determines the {@link Product}
 * associated<br>
 * And for each scanned {@link Product} not already send to Ingest microservice, and with its status equals to
 * {@link ProductStatus#COMPLETED} or {@link ProductStatus#FINISHED},
 * a new {@link JobInfo} of class {@link SIPGenerationJob} is create and queued.
 *
 * @author Christophe Mertz
 *
 */
public class ProductAcquisitionJob extends AbstractJob<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductAcquisitionJob.class);

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    private IProductService productService;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IAcquisitionProcessingChainService acqProcessChainService;

    /**
     * Resolver to retrieve authentication information
     */
    @Autowired
    private IAuthenticationResolver authResolver;

    /**
     * The current {@link AcquisitionProcessingChain}
     */
    private AcquisitionProcessingChain acqProcessingChain;

    @Override
    public void run() {
        LOGGER.info("[{}-{}] : starting acquisition job", acqProcessingChain.getLabel(),
                    acqProcessingChain.getSession());

        try {
            // Step 1 : required files scanning
            IProcessingStep<Void, Void> scanStep = new AcquisitionScanStep(this);
            beanFactory.autowireBean(scanStep);
            scanStep.execute(null);
            // Step 2 : optional files checking
            IProcessingStep<Void, Void> chechStep = new AcquisitionCheckStep(this);
            beanFactory.autowireBean(chechStep);
            chechStep.execute(null);

            // for each complete product, create and queued a Job to generate SIP
            final int n = submitProducts();

            // Job is terminated ... release processing chain
            acqProcessingChain.setRunning(false);
            acqProcessChainService.createOrUpdate(acqProcessingChain);

            LOGGER.info("[{}-{}] : {} jobs for SIP generation queued", acqProcessingChain.getLabel(),
                        acqProcessingChain.getSession(), n);

        } catch (ModuleException pse) {
            LOGGER.error("Business error", pse);
            throw new JobRuntimeException(pse);
        }
    }

    /**
     * Create and queued a {@link JobInfo} of class {@link SIPGenerationJob} for each {@link Product} not
     * already send to ingest microservice,<br>
     * and with its status equals to {@link ProductStatus#COMPLETED} or {@link ProductStatus#FINISHED}.
     * @return the number of {@link JobInfo} create and queued
     */
    private int submitProducts() {
        Set<Product> products = productService.findBySendedAndStatusIn(Boolean.FALSE, ProductStatus.COMPLETED,
                                                                       ProductStatus.FINISHED);
        int nbJobQueued = 0;
        for (Product apr : products) {
            if (scheduleSIPGenerationJob(apr)) {
                nbJobQueued++;
            } else {
                LOGGER.error("error :{}", apr.getProductName());
            }
        }
        return nbJobQueued;
    }

    /**
     * Create and queued a {@link JobInfo} of class {@link SIPGenerationJob} for a {@link Product}.
     * @param product the {@link Product} to process
     * @return true if the {@link JobInfo} is create and queued
     */
    private boolean scheduleSIPGenerationJob(Product product) {
        // Create a ScanJob
        JobInfo acquisition = new JobInfo();
        acquisition.setParameters(new AcquisitionProcessingChainJobParameter(acqProcessingChain),
                                  new ProductJobParameter(product.getProductName()));
        acquisition.setClassName(SIPGenerationJob.class.getName());
        acquisition.setOwner(authResolver.getUser());

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
        if (!AcquisitionProcessingChainJobParameter.isCompatible(param)) {
            throw new JobParameterInvalidException(
                    "Please use ChainGenerationJobParameter in place of JobParameter (this "
                            + "class is here to facilitate your life so please use it.");
        }

        acqProcessingChain = param.getValue();
    }

    public AcquisitionProcessingChain getAcqProcessingChain() {
        return acqProcessingChain;
    }
}
