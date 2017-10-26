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
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionRuntimeException;
import fr.cnes.regards.modules.acquisition.service.step.IStep;

/**
 * cette classe represente un process et regroupe le comportement commun a tous les Process
 * 
 * @author Christophe Mertz
 */
public class AcquisitionProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionProcess.class);

    //    private static final String DEFAULT_LOG_EXTENSION = ".log";

    /**
     * Identifiant systeme du processus
     */
    protected Integer processId;

    //    /**
    //     * Processus courant
    //     */
    //    protected Thread processThread_;

    private ChainGeneration chainGeneration;

    /**
     * Etape courante
     */
    protected IStep currentStep = null;

    //    /**
    //     * Rapport d'activite du processus courant
    //     * 
    //     * @since 1.0
    //     */
    //    protected LogBook report_;

    //    /**
    //     * Le projet du process
    //     * 
    //     * @since 1.0
    //     */
    //    protected String project_;

    //    /**
    //     * Le group utilisateur du process
    //     */
    //    protected String groupName_;

    //    /**
    //     * Information about the process
    //     * 
    //     * @since 1.0
    //     */
    //    protected AbstractProcessInformations processInformations_ = null;

    /**
     * Flag permettant de signaler l'arrêt d'un process
     */
    private boolean stopProcess = false;

    public AcquisitionProcess(ChainGeneration chain) {
        chainGeneration = chain;
    }

    public void stopProcess() throws AcquisitionRuntimeException {
        if (currentStep != null) {
            currentStep.stop();
        }
        //        else {
        //            addEventToReport(CommonRunMessages.getInstance().getMessage("ssalto.service.common.run.stop.last.step"));
        //        }
        stopProcess = true;
    }

    /**
     * met en pause le process
     */
    public void pauseProcess() {
        currentStep.sleep();
    }

    /**
     * relance le process qui a ete mis en pause
     */
    public void resumeProcess() {
        currentStep.resume();
    }

    //    /**
    //     * initialise le process avec les informations sur le rapport d'activite et les informations specifique au service.
    //     * 
    //     * @param pServiceRepositoryConfiguration
    //     *            < ? extends ServiceRepositoryConfiguration>
    //     */
    //    public abstract void initProcess(ServiceRepositoryConfiguration pServiceRepositoryConfiguration)
    //            throws CommonRunException;
    //
    //    /**
    //     * met a jour le status du process en base
    //     * 
    //     * @param pStatus
    //     * @since 1.0
    //     */
    //    public abstract void updateProcessStatus(ProcessStatus pStatus) throws CommonRunException;

    //    /**
    //     * renvoie le logger du type courant du process pour les logs des services
    //     */
    //    protected abstract Logger getLogger();

    //    /**
    //     * Cette methode permet d'initialiser le rapport d'activite c'est la premier methode a appeler apres le
    //     * constructeur. Elle supprimer le contexte NDC et le reinitialise avec le nom de la thread
    //     * 
    //     * @param pServiceRepositoryConfiguration
    //     *            contient les infos permettant d'initialiser les logs du service
    //     * @throws CommonRunException
    //     * @since 1.0
    //     */
    //    protected void initActivityReport(ServiceRepositoryConfiguration pServiceRepositoryConfiguration)
    //            throws CommonRunException {
    //        File directory = new File(pServiceRepositoryConfiguration.getReportDirectory());
    //
    //        String dateLog = DateFormatter.getDateRepresentation(Calendar.getInstance().getTime(),
    //                                                             DateFormatter.FULL_DATE_TIMESTAMP);
    //
    //        try {
    //            report_ = new LogBook(directory, pServiceRepositoryConfiguration.getReportMsgPattern(),
    //                    processThread_.getName() + "_" + dateLog + DEFAULT_LOG_EXTENSION);
    //        }
    //        catch (LogBookException e) {
    //            throw new CommonRunException(e);
    //        }
    //    }

    //    /**
    //     * relache le rapport d'activite et le met a null. Aucune action sur le rapport n'est possible apres l'appel a cette
    //     * methode.
    //     * 
    //     * @FA SIPNG-FA-0987-CN
    //     * @since 1.0
    //     */
    //    protected void releaseActivityReport() {
    //        NDC.clear();
    //        @SuppressWarnings("rawtypes")
    //        Enumeration appenders = report_.getLogbook().getAllAppenders();
    //        while (appenders.hasMoreElements()) {
    //            Appender app = (Appender) appenders.nextElement();
    //            app.close();
    //        }
    //        report_ = null;
    //    }

    //    /**
    //     * demarre le process. Commence par demarrer la thread
    //     * 
    //     * @since 1.0
    //     */
    //    public void startProcess(Boolean pIsSynchronous) {
    //        processThread_.start();
    //        if (pIsSynchronous) {
    //            try {
    //                processThread_.join();
    //            }
    //            catch (InterruptedException e) {
    //                logger_.warn("Process has been interrupted", e);
    //            }
    //        }
    //    }

    //    private void initConnection() throws CommonRunException {
    //        ConnectionConfiguration.setService(getService());
    //        if (project_ == null) {
    //            addErrorToReport(CommonRunMessages.getInstance()
    //                    .getMessage("ssalto.service.common.run.project.uninitialized"));
    //            throw new CommonRunException();
    //        } // else
    //        ConnectionConfiguration.setProject(project_);
    //        if (groupName_ != null) {
    //            ConnectionConfiguration.setGroup(groupName_);
    //        }
    //    }

    public void run() {
        try {
            //            ProcessManager.getInstance().lockProcess(getLockKey());
            //            // connection information needs to be reinitialized
            //            initNDC();
            //            initConnection();
            while (!stopProcess && (currentStep != null)) {
                currentStep.setProcess(this);
                currentStep.run();
                setCurrentStep(currentStep.getNextStep());
            }
            //            // Compute process status
            //            if (!isProcessFinished()) {
            //                setProcessInterruptedStatus();
            //            }
        } catch (ModuleException e) {
            LOGGER.error(e.getMessage(), e);
            //            try {
            //                setProcessErrorStatus();
            //            }
            //            catch (CommonRunException e1) {
            //                logger_.error(e1.getMessage(), e1);
            //            }
            //            addErrorToReport(CommonRunMessages.getInstance().getMessage("ssalto.service.common.run.unexpected.error",
            //                                                                        e.getMessage()), e);
        }
        //        finally {
        //            // Stop process and remove it from manager
        //            try {
        //                ProcessType processType = null;
        //                if (processInformations_.getClass().getName().equals(AcquisitionProcessInformations.class.getName())) {
        //                    processType = ProcessType.ACQUISITION;
        //                }
        //                else
        //                    if (processInformations_.getClass().getName().equals(ArchivingProcessInformations.class.getName())) {
        //                        processType = ProcessType.ARCHIVING;
        //                    }
        //                    else
        //                        if (processInformations_.getClass().getName()
        //                                .equals(ArchiveCleaningProcessInformations.class.getName())) {
        //                            processType = ProcessType.ARCHIVE_CLEANING;
        //                        }
        //                        else
        //                            if (processInformations_.getClass().getName()
        //                                    .equals(CatalogueUpdateProcessInformations.class.getName())) {
        //                                processType = ProcessType.CATALOGUE_UPDATE;
        //                            }
        //                            else
        //                                if (processInformations_.getClass().getName()
        //                                        .equals(DeletionProcessInformations.class.getName())) {
        //                                    processType = ProcessType.DELETION;
        //                                }
        //                ProcessManager.getInstance().stopProcess(processId_, processType);
        //                ProcessManager.getInstance().unlockProcess(getLockKey());
        //                System.gc();
        //            }
        //            catch (CommonRunException e) {
        //                logger_.error("", e);
        //                addErrorToReport(CommonRunMessages.getInstance().getMessage("ssalto.service.common.run.stop.error",
        //                                                                            processThread_.getName()), e);
        //            }
        //            catch (Exception e) {
        //                logger_.error("", e);
        //                addErrorToReport(CommonRunMessages.getInstance()
        //                                         .getMessage("ssalto.service.common.run.unexpected.error", e.getMessage()), e);
        //            }
        //        }
    }

    public ChainGeneration getChainGeneration() {
        return chainGeneration;
    }

    public void setChainGeneration(ChainGeneration chainGeneration) {
        this.chainGeneration = chainGeneration;
    }

    public IStep getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(IStep currentStep) {
        this.currentStep = currentStep;
    }

    //    protected void initNDC() {
    //        NDC.clear();
    //        NDC.push(processThread_.getName());
    //    }
    //
    //    public void addEventToReport(String message) {
    //        if (report_ != null) {
    //            report_.info(message, logger_);
    //        }
    //        else {
    //            getLogger().info(message);
    //        }
    //    }
    //
    //    public void addErrorToReport(String message) {
    //        if (report_ != null) {
    //            report_.error(message, logger_);
    //        }
    //        else {
    //            getLogger().error(message);
    //        }
    //    }
    //
    //    public void addErrorToReport(String message, Throwable pThrowable) {
    //        if (report_ != null) {
    //            report_.error(message, logger_, pThrowable);
    //        }
    //        else {
    //            getLogger().error(message, pThrowable);
    //        }
    //    }
    //
    //    public void addWarnToReport(String message) {
    //        if (report_ != null) {
    //            report_.warn(message, logger_);
    //        }
    //        else {
    //            getLogger().warn(message);
    //        }
    //    }
    //
    //    public void addWarnToReport(String message, Throwable pThrowable) {
    //        if (report_ != null) {
    //            report_.warn(message, logger_, pThrowable);
    //        }
    //        else {
    //            getLogger().warn(message, pThrowable);
    //        }
    //    }
    //
    //    /**
    //     * Positionne le process a l'etat RUNNING_WARNING lorsqu'une anomalie a ete detecte sur un fichier.
    //     * 
    //     * @throws CommonRunException
    //     * @since 1.2
    //     */
    //    public void setProcessWarningStatus() throws CommonRunException {
    //        updateProcessStatus(ProcessStatus.RUNNING_WARNING);
    //    }
    //
    //    /**
    //     * Positionne le process a l'etat RUNNING_ERROR lorsqu'une exception est levee au niveau process.
    //     * 
    //     * @throws CommonRunException
    //     * @since 1.0
    //     */
    //    private void setProcessErrorStatus() throws CommonRunException {
    //        updateProcessStatus(ProcessStatus.RUNNING_ERROR);
    //    }
    //
    //    /**
    //     * Positionne le process a l'etat RUNNING_INTERRUPTED lorsqu'une exception est levee au niveau process.
    //     * 
    //     * @throws CommonRunException
    //     * @since 1.0
    //     */
    //    private void setProcessInterruptedStatus() throws CommonRunException {
    //        updateProcessStatus(ProcessStatus.RUNNING_INTERRUPTED);
    //    }
    //
    //    /**
    //     * Verifie via le pourcentage de progression si l'acquisition est terminee ou non
    //     * 
    //     * @return true si le pourcentage de progression est egal a 100
    //     * @since 1.0
    //     */
    //    private boolean isProcessFinished() {
    //        int progress = 0;
    //        if ((processInformations_ != null) && (processInformations_.getProgress() != null)) {
    //            progress = processInformations_.getProgress().intValue();
    //        }
    //        return progress == 100;
    //
    //    }

}