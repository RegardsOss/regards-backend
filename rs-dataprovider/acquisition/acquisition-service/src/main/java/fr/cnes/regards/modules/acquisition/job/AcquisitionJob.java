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

package fr.cnes.regards.modules.acquisition.job;

import java.time.OffsetDateTime;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.job.ChainGenerationJobParameter;
import fr.cnes.regards.modules.acquisition.job.step.IAcquisitionScanStep;
import fr.cnes.regards.modules.acquisition.job.step.IStep;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionRuntimeException;

/**
 * @author Christophe Mertz
 *
 */
public class AcquisitionJob extends AbstractJob<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionJob.class);

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    private IAcquisitionScanStep acquisitionScanStep;

    @Override
    public void run() {
        Set<JobParameter> chains = getParameters();
        ChainGeneration chainGeneration = chains.iterator().next().getValue();

        LOGGER.info("Start acquisition job for the chain <" + chainGeneration.getLabel() + ">");

        // The MetaProduct is required
        if (chainGeneration.getMetaProduct() == null) {
            throw new AcquisitionRuntimeException(
                    "The required MetaProduct is missing for the ChainGeneration <" + chainGeneration.getLabel() + ">");
        }

        ProcessGeneration process = new ProcessGeneration(chainGeneration);

        // AcquisitionStep is the first step
        IStep firstStep = acquisitionScanStep;
        firstStep.setProcess(process);
        beanFactory.autowireBean(firstStep);
        process.setCurrentStep(firstStep);

        // CheckStep is the second step
        // configurer les steps        
        // firstStep.setNextStep(totoStep);

        try {
            chainGeneration.setLastDateActivation(OffsetDateTime.now());
            firstStep.proceedStep();
        } catch (AcquisitionRuntimeException e) {
            LOGGER.error(e.getMessage());
            throw e;
        }

        LOGGER.info("End  acquisition job for the chain <" + chainGeneration.getLabel() + ">");
    }

    @Override
    public void setParameters(Set<JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        if (parameters.isEmpty()) {
            throw new JobParameterMissingException("No parameter provided");
        }
        if (parameters.size() != 1) {
            throw new JobParameterInvalidException("Only one parameter is expected.");
        }
        JobParameter param = parameters.iterator().next();
        if (!ChainGenerationJobParameter.isCompatible(param)) {
            throw new JobParameterInvalidException(
                    "Please use ChainGenerationJobParameter in place of JobParameter (this "
                            + "class is here to facilitate your life so please use it.");
        }
        super.parameters = parameters;
    }

}
