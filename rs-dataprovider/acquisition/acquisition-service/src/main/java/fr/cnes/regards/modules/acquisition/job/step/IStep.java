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
package fr.cnes.regards.modules.acquisition.job.step;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.job.AcquisitionProcess;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionException;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionRuntimeException;

/**
 * 
 * @author Christophe Mertz
 */
public interface IStep {

    /**
     * methode dans laquelle l'etape initialise les ressources dont elle aura besoin lors de son execution, comme des
     * connexion bd, des digester, des connections ftp etc...
     */
    public void getResources() throws AcquisitionException;

    /**
     * permet de liberer les resources allouees dans getResources()
     */
    public void freeResources() throws AcquisitionException;

    /**
     * permet d'arreter l'etape dans un etat correct
     */
    public void stop();

    /**
     * permet de reprendre l'execution de l'etape.
     */
    public void resume();

    /**
     * dans cette methode se trouve le code qui execute les instructions de l'etape
     */
    public void proceedStep() throws AcquisitionRuntimeException;

    /**
     * permet de mettre l'etape en veille pour pouvoir reprendre l'execution plus tard.
     */
    public void sleep();

    public String getName();

    public void run() throws ModuleException;

    public IStep getNextStep();

    public void setNextStep(IStep step);

    public void setProcess(AcquisitionProcess process);

}
