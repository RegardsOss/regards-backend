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
 * This interface defines a step to execute by an {@link AcquisitionProcess}.<br>
 * A step runs treatments and used resources. 
 * 
 * @author Christophe Mertz
 */
public interface IStep {

    /**
     * This method is used to initialize the resources required for the {@link IStep} 
     */
    public void getResources() throws AcquisitionException;

    /**
     * This method aims to free reources used by the {@link IStep}
     */
    public void freeResources() throws AcquisitionException;

    /**
     * This method aims to stop properly the {@link IStep} execution
     */
    public void stop();

    /**
     * This method aims to run the treatments for the {@link IStep}  
     */
    public void proceedStep() throws AcquisitionRuntimeException;

    public String getName();

    /**
     * This method chains the methods getResources,  proceedStep and freeResources
     * @throws ModuleException
     */
    public void run() throws ModuleException;

    /**
     * Return the next {@link IStep} to execute after the current {@link IStep}
     * @return the next {@link IStep}
     */
    public IStep getNextStep();

    /**
     * Set the next {@link IStep} to execute after the current {@link IStep}
     * @param step
     */
    public void setNextStep(IStep step);

    /**
     * Set the {@link AcquisitionProcess} that shoul execute the current {@link IStep}
     * @param process the {@link AcquisitionProcess}
     */
    public void setProcess(AcquisitionProcess process);

}
