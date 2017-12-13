/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service.job;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.storage.domain.StorageException;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IWorkingSubset;

/**
 * Abstract job that allows to handle most of storage operations.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public abstract class AbstractStoreFilesJob extends AbstractJob<Void> {

    /**
     * Job parameter name for the Data storage plugin configuration id to use
     */
    public static final String PLUGIN_TO_USE_PARAMETER_NAME = "pluginToUseId";

    /**
     * Job parameter name for the working subset
     */
    public static final String WORKING_SUB_SET_PARAMETER_NAME = "workingSubSet";

    /**
     * Not handled data file message
     */
    protected static final String NOT_HANDLED_MSG = "This data file has not been handled by the designated DataStorage";

    /**
     * Failure causes message format
     */
    protected static final String FAILURE_CAUSES = "Storage failed due to the following reasons: %s";

    /**
     * Job parameter missing message format
     */
    protected static final String PARAMETER_MISSING = "%s requires a %s as \"%s\" parameter";

    /**
     * Job parameter invalid message format
     */
    protected static final String PARAMETER_INVALID = "%s requires a valid %s(identifier: %s)";

    /**
     * Class logger
     */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * {@link IPluginService} instance
     */
    @Autowired
    protected IPluginService pluginService;

    /**
     * {@link IPublisher} instance
     */
    @Autowired
    protected IPublisher publisher;

    /**
     * The progress manager allowing to get a communication between the job, the plugin and the AIPService
     */
    protected StorageJobProgressManager progressManager;

    /**
     * The job parameters as a map
     */
    protected Map<String, JobParameter> parameters;

    /**
     * Check that the given job parameters contains required parameters and that they are valid.
     *
     * @return a map which keys are the job parameter name and its value the job parameter. This map contains no entry
     *         if there is no parameter provided.
     * @throws JobParameterMissingException
     * @throws JobParameterInvalidException
     */
    protected void checkParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {

        // lets see if the plugin to use has been given through a plugin configuration.
        JobParameter pluginToUseId;
        if (((pluginToUseId = parameters.get(PLUGIN_TO_USE_PARAMETER_NAME)) == null) || !(pluginToUseId
                .getValue() instanceof Long)) {
            JobParameterMissingException e = new JobParameterMissingException(String.format(PARAMETER_MISSING,
                                                                                            this.getClass().getName(),
                                                                                            Long.class.getName(),
                                                                                            PLUGIN_TO_USE_PARAMETER_NAME));
            logger.error(e.getMessage(), e);
            throw e;
        }
        // now lets check it is an id of a plugin configuration
        Long confId = pluginToUseId.getValue();
        // rather than duplicating the code, lets just catch the exception
        ModuleException noConfigurationExist = null;
        PluginConfiguration confToUse = null;
        try {
            confToUse = pluginService.getPluginConfiguration(confId);
        } catch (ModuleException e) {
            JobParameterInvalidException invalid = new JobParameterInvalidException(String.format(
                    "Job %s: There is no plugin configuration with id: %s",
                    this.getClass(),
                    confId), e);
            logger.error(invalid.getMessage(), invalid);
            throw invalid;
        }
        // now that we are sure there is a plugin configuration as parameter, lets check if its a plugin configuration
        // of IDataStorage.
        if (!confToUse.getInterfaceNames().contains(IDataStorage.class.getName())) {
            JobParameterInvalidException e = new JobParameterInvalidException(String.format(PARAMETER_INVALID,
                                                                                            this.getClass().getName(),
                                                                                            IDataStorage.class.getName()
                                                                                                    + " configuration",
                                                                                            confId));
            logger.error(e.getMessage(), e);
            throw e;
        }
        JobParameter workingSubSet;
        if (((workingSubSet = parameters.get(WORKING_SUB_SET_PARAMETER_NAME)) == null) || !(workingSubSet
                .getValue() instanceof IWorkingSubset)) {
            JobParameterMissingException e = new JobParameterMissingException(String.format(PARAMETER_MISSING,
                                                                                            this.getClass().getName(),
                                                                                            IWorkingSubset.class
                                                                                                    .getName(),
                                                                                            WORKING_SUB_SET_PARAMETER_NAME));
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        checkParameters(parameters);
        this.parameters = parameters;
    }

    @Override
    public void run() {
        progressManager = new StorageJobProgressManager(publisher, this);
        try {
            doRun(parameters);
        } finally {
            logger.debug("[FILE JOB] Executing some checks after execution");
            // eventually, lets see if everything went as planned
            afterRun();
        }
    }

    /**
     * do the actual storage according to the parameters
     *
     * @param parameterMap parsed parameters
     */
    protected abstract void doRun(Map<String, JobParameter> parameterMap);

    /**
     * Decides if the job should fail or not
     */
    protected void afterRun() {
        // before we make the job fail, lets check if all DataFile have been handled
        Collection<DataFile> handled = progressManager.getHandledDataFile();
        IWorkingSubset workingSubset = parameters.get(WORKING_SUB_SET_PARAMETER_NAME).getValue();
        if (!handled.containsAll(workingSubset.getDataFiles())) {
            // not all data files have been handled, lets get the difference and make the not handled fail
            Sets.SetView<DataFile> notHandledFiles = Sets
                    .difference(workingSubset.getDataFiles(), Sets.newHashSet(handled));
            for (DataFile notHandled : notHandledFiles) {
                handleNotHandledDataFile(notHandled);
            }
        }
        if (progressManager.isProcessError()) {
            // RuntimeException allows us to make the job fail and respect Runnable interface
            throw new StorageException(String.format(FAILURE_CAUSES,
                                                     progressManager.getFailureCauses().stream()
                                                             .collect(Collectors.joining(", ", "[", " ]"))));
        }
    }

    /**
     * Method called when the job detects files that have not been handled by the storage plugin.
     * @param notHandled a data file that have not been handled by the storage plugin
     */
    protected abstract void handleNotHandledDataFile(DataFile notHandled);

    /**
     * Store files thanks to the parametrized data storage. Indicated to the data storage if the file should be replaced or not
     * @param parameterMap
     * @param replaceMode
     */
    @SuppressWarnings("unchecked")
    protected void storeFile(Map<String, JobParameter> parameterMap, boolean replaceMode) {
        // lets instantiate the plugin to use
        Long confIdToUse = parameterMap.get(PLUGIN_TO_USE_PARAMETER_NAME).getValue();
        try {
            @SuppressWarnings("rawtypes") IDataStorage storagePlugin = pluginService.getPlugin(confIdToUse);
            // now that we have the plugin instance, lets retrieve the aip from the job parameters and ask the plugin to
            // do the storage
            IWorkingSubset workingSubset = parameterMap.get(WORKING_SUB_SET_PARAMETER_NAME).getValue();
            // before storage on file system, lets update the DataFiles by setting which data storage is used to storeAndCreate
            // them.
            PluginConfiguration storagePluginConf = pluginService.getPluginConfiguration(confIdToUse);
            for (DataFile data : workingSubset.getDataFiles()) {
                data.setDataStorageUsed(storagePluginConf);
            }
            storagePlugin.store(workingSubset, replaceMode, progressManager);
        } catch (ModuleException e) {
            // throwing new runtime allows us to make the job fail.
            throw new JobRuntimeException(e);
        }
    }

    @Override
    public int getCompletionCount() {
        return ((IWorkingSubset) parameters.get(WORKING_SUB_SET_PARAMETER_NAME).getValue()).getDataFiles().size();
    }

    /**
     * @return the progress manager
     */
    public StorageJobProgressManager getProgressManager() {
        return progressManager;
    }
}
