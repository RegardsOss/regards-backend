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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionException;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionRuntimeException;
import fr.cnes.regards.modules.acquisition.service.job.AcquisitionProcess;

/**
 * This abstract class implements the interface {@link IStep} and defines the method {@link IStep#run()}.
 * 
 * @author Christophe Mertz
 */
public abstract class AbstractStep implements IStep {

    protected IStep nextStep;

    protected AcquisitionProcess process;

    public AbstractStep() {
        super();
    }

    /**
     * This method chains the methods getResources,  proceedStep and freeResources
     * @throws ModuleException
     */
    public void run() throws ModuleException {
        getResources();
        proceedStep();
        freeResources();
    }

    /**
     * This method is used to initialize the resources required for the {@link IStep} 
     */
    public abstract void getResources() throws AcquisitionException;

    /**
     * This method aims to free reources used by the {@link IStep}
     */
    public abstract void freeResources() throws AcquisitionException;

    /**
     * This method aims to run the treatments for the {@link IStep}  
     */
    public abstract void proceedStep() throws AcquisitionRuntimeException;

    @Override
    public IStep getNextStep() {
        return nextStep;
    }

    public void setNextStep(IStep nextStep) {
        this.nextStep = nextStep;
    }

    public AcquisitionProcess getProcess() {
        return process;
    }

    @Override
    public void setProcess(AcquisitionProcess process) {
        this.process = process;
    }

}
