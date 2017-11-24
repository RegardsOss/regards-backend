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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionRuntimeException;
import fr.cnes.regards.modules.acquisition.service.step.IStep;

/**
 * This class aims to execute a sequence of {@link IStep}.
 * 
 * @author Christophe Mertz
 */
public class AcquisitionProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionProcess.class);

    private final ChainGeneration chainGeneration;

    private Product product;

    /**
     * The current {@link IStep} to execute.
     */
    protected IStep currentStep = null;

    /**
     * A flag to indicate that the process should be stopped.<br>
     * The current {@link IStep} is stopped and the next step should not be executed. 
     */
    private boolean stopProcess = false;

    public AcquisitionProcess(ChainGeneration chain) {
        chainGeneration = chain;
    }

    public AcquisitionProcess(ChainGeneration chain, Product aProduct) {
        chainGeneration = chain;
        product = aProduct;
    }

    public void stopProcess() throws AcquisitionRuntimeException {
        if (currentStep != null) {
            currentStep.stop();
        }
        stopProcess = true;
    }

    public void run() {
        try {
            while (!stopProcess && (currentStep != null)) {
                currentStep.setProcess(this);
                currentStep.run();
                setCurrentStep(currentStep.getNextStep());
            }
        } catch (ModuleException e) {
            String msg = "["+ chainGeneration.getSession()+ "]";
            LOGGER.error(msg, e);
        }
    }

    public ChainGeneration getChainGeneration() {
        return chainGeneration;
    }

    public Product getProduct() {
        return product;
    }

    public IStep getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(IStep currentStep) {
        this.currentStep = currentStep;
    }
}