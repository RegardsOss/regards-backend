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
import fr.cnes.regards.modules.storage.plugin.datastorage.IDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.IWorkingSubset;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public abstract class AbstractStoreFilesJob extends AbstractJob<Void> {

    public static final String PLUGIN_TO_USE_PARAMETER_NAME = "pluginToUse";

    public static final String WORKING_SUB_SET_PARAMETER_NAME = "workingSubSet";

    protected static final String NOT_HANDLED_MSG = "This data file has not been handled by the designated DataStorage";

    protected static final String FAILURE_CAUSES = "Storage failed due to the following reasons: %s";

    protected static final String PARAMETER_MISSING = "%s requires a %s as \"%s\" parameter";

    protected static final String PARAMETER_INVALID = "%s requires a valid %s(identifier: %s)";

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected IPluginService pluginService;

    @Autowired
    protected IPublisher publisher;

    protected StorageJobProgressManager progressManager;

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
        JobParameter pluginToUse;
        if (((pluginToUse = parameters.get(PLUGIN_TO_USE_PARAMETER_NAME)) == null)
                || !(pluginToUse.getValue() instanceof PluginConfiguration)) {
            JobParameterMissingException e = new JobParameterMissingException(
                    String.format(PARAMETER_MISSING, this.getClass().getName(), PluginConfiguration.class.getName(),
                                  PLUGIN_TO_USE_PARAMETER_NAME));
            logger.error(e.getMessage(), e);
            throw e;
        }
        // now that we are sure there is a plugin configuration as parameter, lets check if its a plugin configuration
        // of IDataStorage
        PluginConfiguration confToUse = pluginToUse.getValue();
        if (!confToUse.getInterfaceNames().contains(IDataStorage.class.getName())) {
            JobParameterInvalidException e = new JobParameterInvalidException(
                    String.format(PARAMETER_INVALID, this.getClass().getName(),
                                  IDataStorage.class.getName() + " configuration", confToUse.getId()));
            logger.error(e.getMessage(), e);
            throw e;
        }
        JobParameter workingSubSet;
        if (((workingSubSet = parameters.get(WORKING_SUB_SET_PARAMETER_NAME)) == null)
                || !(workingSubSet.getValue() instanceof IWorkingSubset)) {
            JobParameterMissingException e = new JobParameterMissingException(
                    String.format(PARAMETER_MISSING, this.getClass().getName(), IWorkingSubset.class.getName(),
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
            Sets.SetView<DataFile> notHandledFiles = Sets.difference(workingSubset.getDataFiles(),
                                                                     Sets.newHashSet(handled));
            for (DataFile notHandled : notHandledFiles) {
                handleNotHandledDataFile(notHandled);
            }
        }
        if (progressManager.isProcessError()) {
            // RuntimeException allows us to make the job fail and respect Runnable interface
            throw new StorageException(String
                    .format(FAILURE_CAUSES,
                            progressManager.getFailureCauses().stream().collect(Collectors.joining(", ", "[", " ]"))));
        }
    }

    /**
     * Method called when the job detects files that have not been handled by the storage plugin.
     * @param notHandled a data file that have not been handled by the storage plugin
     */
    protected abstract void handleNotHandledDataFile(DataFile notHandled);

    @SuppressWarnings("unchecked")
    protected void storeFile(Map<String, JobParameter> parameterMap, boolean replaceMode) {
        // lets instantiate the plugin to use
        PluginConfiguration confToUse = parameterMap.get(PLUGIN_TO_USE_PARAMETER_NAME).getValue();
        try {
            @SuppressWarnings("rawtypes")
            IDataStorage storagePlugin = pluginService.getPlugin(confToUse.getId());
            // now that we have the plugin instance, lets retrieve the aip from the job parameters and ask the plugin to
            // do the storage
            IWorkingSubset workingSubset = parameterMap.get(WORKING_SUB_SET_PARAMETER_NAME).getValue();
            // before storage on file system, lets update the DataFiles by setting which data storage is used to store
            // them.
            for (DataFile data : workingSubset.getDataFiles()) {
                data.setDataStorageUsed(confToUse);
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

    public StorageJobProgressManager getProgressManager() {
        return progressManager;
    }
}
