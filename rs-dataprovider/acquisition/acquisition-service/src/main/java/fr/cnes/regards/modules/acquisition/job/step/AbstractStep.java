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
public abstract class AbstractStep implements IStep {

    // TODO CMZ à utiliser
    protected int state;

    protected IStep nextStep;

    protected AcquisitionProcess process;

    public AbstractStep() {
        super();
    }

    public void run() throws ModuleException {
        getResources();
        proceedStep();
        freeResources();
    }

    /**
     * methode dans laquelle l'etape initialise les ressources dont elle aura besoin lors de son execution, comme des
     * connexion bd, des digester, des connections ftp etc...
     */
    public abstract void getResources() throws AcquisitionException;

    /**
     * permet de liberer les resources allouees dans getResources()
     */
    public abstract void freeResources() throws AcquisitionException;

    /**
     * permet d'arreter l'etape dans un etat correct
     */
    public abstract void stop();

    /**
     * permet de reprendre l'execution de l'etape.
     */
    public abstract void resume();

    /**
     * dans cette methode se trouve le code qui execute les instructions de l'etape
     */
    public abstract void proceedStep() throws AcquisitionRuntimeException;

    //    /**
    //     * met a jour l'attribut progress en base
    //     * 
    //     * @param pProgress
    //     *            : l'indice de progression ( entre 0 et 100)
    //     * @throws CommonRunException
    //     *             en cas d'erreur lors de l'update en base
    //     * @since 1.1
    //     * @DM SIPNG-DM-0044-CN : creation
    //     */
    //    protected void updateProcessProgress(int pProgress) throws AcquisitionException {
    //        try {
    //            AbstractProcessInformations abstractProcessInformations = process_.getProcessInformations();
    //            abstractProcessInformations.setProgress(new Integer(pProgress));
    //            SsaltoControlers.getControler(abstractProcessInformations).update(abstractProcessInformations);
    //        }
    //        catch (SsaltoDomainException e) {
    //            String exceptionMsg = CommonRunMessages.getInstance()
    //                    .getMessage("ssalto.service.archiving.run.updating.progress.error");
    //            process_.addErrorToReport(exceptionMsg, e);
    //            throw new CommonRunException(exceptionMsg);
    //        }
    //    }

    /**
     * permet de mettre l'etape en veille pour pouvoir reprendre l'execution plus tard.
     */
    public abstract void sleep();

    // TODO CMZ util ?
    public abstract String getName();

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

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
