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
import fr.cnes.regards.modules.acquisition.domain.job.ChainGenerationJobParameter;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionRuntimeException;
import fr.cnes.regards.modules.acquisition.service.step.IAcquisitionCheckStep;
import fr.cnes.regards.modules.acquisition.service.step.IAcquisitionScanStep;
import fr.cnes.regards.modules.acquisition.service.step.IGenerateSipStep;
import fr.cnes.regards.modules.acquisition.service.step.IStep;

/**
 * @author Christophe Mertz
 *
 */
public class AcquisitionJob extends AbstractJob<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionJob.class);

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    private IAcquisitionScanStep scanStepImpl;

    @Autowired
    private IAcquisitionCheckStep checkStepImpl;

    @Autowired
    private IGenerateSipStep generateSIPStepImpl;
    
    private ChainGeneration chainGeneration;

    @Override
    public void run() {
        IStep checkStep = null;
        IStep generateSipStep = null;

        LOGGER.info("Start acquisition job for the chain <{}>", chainGeneration.getLabel());

        // The MetaProduct is required
        if (chainGeneration.getMetaProduct() == null) {
            throw new AcquisitionRuntimeException(
                    "The required MetaProduct is missing for the ChainGeneration <" + chainGeneration.getLabel() + ">");
        }

        AcquisitionProcess process = new AcquisitionProcess(chainGeneration);

        // IAcquisitionScanStep is the first step, it is mandatory
        IStep scanStep = scanStepImpl;
        scanStep.setProcess(process);
        beanFactory.autowireBean(scanStep);
        process.setCurrentStep(scanStep);

        // IAcquisitionCheckStep is optional
        if (chainGeneration.getCheckAcquisitionPluginConf() != null) {
            checkStep = checkStepImpl;
            checkStep.setProcess(process);
            beanFactory.autowireBean(checkStep);
            scanStep.setNextStep(checkStep);
        }

        // IGenerateSIPStep is mandatory
        generateSipStep = generateSIPStepImpl;
        generateSipStep.setProcess(process);
        beanFactory.autowireBean(generateSipStep);
        if (checkStep != null) {
            checkStep.setNextStep(generateSipStep);
        } else {
            scanStep.setNextStep(generateSipStep);
        }

        process.run();

        LOGGER.info("End  acquisition job for the chain <{}>", chainGeneration.getLabel());
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
