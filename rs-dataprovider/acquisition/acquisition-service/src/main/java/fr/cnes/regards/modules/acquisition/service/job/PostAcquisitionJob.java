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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.job.SIPEventJobParameter;
import fr.cnes.regards.modules.acquisition.service.IChainGenerationService;
import fr.cnes.regards.modules.acquisition.service.IProductService;
import fr.cnes.regards.modules.acquisition.service.step.IPostAcquisitionStep;
import fr.cnes.regards.modules.acquisition.service.step.IStep;
import fr.cnes.regards.modules.acquisition.service.step.PostSipAcquisitionStep;
import fr.cnes.regards.modules.ingest.domain.event.SIPEvent;

/**
 * This job runs a set of step :<br>
 * <li>a step {@link IPostAcquisitionStep}
 * 
 * This job runs for one {@link Product} 
 *  
 * @author Christophe Mertz
 *
 */
public class PostAcquisitionJob extends AbstractJob<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostAcquisitionJob.class);

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    private IProductService productService;

    @Autowired
    private IChainGenerationService chainGenerationService;

    private SIPEvent sipEvent;

    @Override
    public void run() {
        LOGGER.info("Start POST acquisition SIP job for the product <{}>", sipEvent.getIpId());

        Product product = productService.retrieve(sipEvent.getIpId());
        ChainGeneration chainGeneration = chainGenerationService.findByMetaProduct(product.getMetaProduct());

        AcquisitionProcess process = new AcquisitionProcess(chainGeneration, product);

        // IPostAcquisitionStep is the first step
        IStep postSipStep = new PostSipAcquisitionStep(sipEvent);
        postSipStep.setProcess(process);
        beanFactory.autowireBean(postSipStep);
        process.setCurrentStep(postSipStep);

        process.run();
    }

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        if (parameters.isEmpty()) {
            throw new JobParameterMissingException("No parameter provided");
        }
        if (parameters.size() != 1) {
            throw new JobParameterInvalidException("One parameter is expected");
        }

        for (JobParameter jp : parameters.values()) {
            if (SIPEventJobParameter.isCompatible(jp)) {
                sipEvent = jp.getValue();
            } else {
                throw new JobParameterInvalidException("Please use SIPEventJobParameter in place of JobParameter");
            }
        }
    }

}
