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
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductStatus;
import fr.cnes.regards.modules.acquisition.domain.job.ChainGenerationJobParameter;
import fr.cnes.regards.modules.acquisition.domain.job.ProductJobParameter;
import fr.cnes.regards.modules.acquisition.service.IProductService;
import fr.cnes.regards.modules.acquisition.service.step.GenerateSipStep;
import fr.cnes.regards.modules.acquisition.service.step.IGenerateSipStep;
import fr.cnes.regards.modules.acquisition.service.step.IStep;

/**
 * This job runs a set of step :<br>
 * <li>a step {@link GenerateSipStep} to generate a SIP for a {@link Product}
 * <li>a step of preprocessing 
 * 
 * This job runs for one {@link Product} 
 *  
 * @author Christophe Mertz
 *
 */
public class AcquisitionGenerateSIPJob extends AbstractJob<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionGenerateSIPJob.class);

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    private IGenerateSipStep generateSIPStepImpl;

    private ChainGeneration chainGeneration;

    private Product product;

    @Override
    public void run() {
        LOGGER.info("Start generate SIP job for the product <{}> of the chain <{}>", product.getProductName(),
                    chainGeneration.getLabel());

        AcquisitionProcess process = new AcquisitionProcess(chainGeneration, product);

        // IAcquisitionScanStep is the first step
        IStep generateSIPStep = generateSIPStepImpl;
        generateSIPStep.setProcess(process);
        beanFactory.autowireBean(generateSIPStep);
        process.setCurrentStep(generateSIPStep);

        // TODO CMZ preprocessing

        process.run();
        
        LOGGER.info("Start generate SIP job for the product <{}> of the chain <{}>", product.getProductName(),
                    chainGeneration.getLabel());
    }

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        if (parameters.isEmpty()) {
            throw new JobParameterMissingException("No parameter provided");
        }
        if (parameters.size() != 1) {
            throw new JobParameterInvalidException("Two parameters are expected.");
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
