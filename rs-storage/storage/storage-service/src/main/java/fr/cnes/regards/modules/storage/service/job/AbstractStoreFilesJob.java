/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service.job;

import java.awt.Dimension;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
import fr.cnes.regards.framework.utils.file.CommonFileUtils;
import fr.cnes.regards.framework.utils.file.DownloadUtils;
import fr.cnes.regards.modules.storage.domain.StorageException;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
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
        if (((pluginToUseId = parameters.get(PLUGIN_TO_USE_PARAMETER_NAME)) == null)
                || !(pluginToUseId.getValue() instanceof Long)) {
            JobParameterMissingException e = new JobParameterMissingException(
                    String.format(PARAMETER_MISSING, this.getClass().getName(), Long.class.getName(),
                                  PLUGIN_TO_USE_PARAMETER_NAME));
            logger.error(e.getMessage(), e);
            throw e;
        }
        // now lets check it is an id of a plugin configuration
        Long confId = pluginToUseId.getValue();
        // rather than duplicating the code, lets just catch the exception
        PluginConfiguration confToUse = null;
        try {
            confToUse = pluginService.getPluginConfiguration(confId);
        } catch (ModuleException e) {
            JobParameterInvalidException invalid = new JobParameterInvalidException(
                    String.format("Job %s: There is no plugin configuration with id: %s", this.getClass(), confId), e);
            logger.error(invalid.getMessage(), invalid);
            throw invalid;
        }
        // now that we are sure there is a plugin configuration as parameter, lets check if its a plugin configuration
        // of IDataStorage.
        if (!confToUse.getInterfaceNames().contains(IDataStorage.class.getName())) {
            JobParameterInvalidException e = new JobParameterInvalidException(
                    String.format(PARAMETER_INVALID, this.getClass().getName(),
                                  IDataStorage.class.getName() + " configuration", confId));
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
        progressManager = new StorageJobProgressManager(publisher, this,
                (Long) parameters.get(PLUGIN_TO_USE_PARAMETER_NAME).getValue());
        try {
            doRun(parameters);
        } catch (RuntimeException e) {
            logger.error(e.getMessage(), e);
            throw e;
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
        // before we make the job fail, lets check if all StorageDataFile have been handled
        Collection<StorageDataFile> handled = progressManager.getHandledDataFile();
        IWorkingSubset workingSubset = parameters.get(WORKING_SUB_SET_PARAMETER_NAME).getValue();
        // ... by making the difference between the two set and check if the result is empty
        Sets.SetView<StorageDataFile> notHandledFiles = Sets.difference(workingSubset.getDataFiles(),
                                                                        Sets.newHashSet(handled));
        if (!notHandledFiles.isEmpty()) {
            // not all data files have been handled, lets get the difference and make the not handled fail
            for (StorageDataFile notHandled : notHandledFiles) {
                handleNotHandledDataFile(notHandled);
            }
        }
        if (!handled.containsAll(workingSubset.getDataFiles())) {
            // not all data files have been handled, lets get the difference and make the not handled fail
            Sets.SetView<StorageDataFile> notHandledFiles = Sets.difference(workingSubset.getDataFiles(),
                                                                            Sets.newHashSet(handled));
            for (StorageDataFile notHandled : notHandledFiles) {
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
    protected abstract void handleNotHandledDataFile(StorageDataFile notHandled);

    /**
     * Store files thanks to the parametrized data storage. Indicated to the data storage if the file should be replaced or not
     * @param parameterMap
     * @param replaceMode
     */
    @SuppressWarnings("unchecked")
    protected void storeFile(Map<String, JobParameter> parameterMap, boolean replaceMode) {
        // lets instantiate the plugin to use
        Long confIdToUse = parameterMap.get(PLUGIN_TO_USE_PARAMETER_NAME).getValue();
        // before doing anything, lets get the working set and set the quicklooks into the job workspace to get their size
        IWorkingSubset workingSubset = parameterMap.get(WORKING_SUB_SET_PARAMETER_NAME).getValue();
        Set<StorageDataFile> dataFilesNotToStore = Sets.newHashSet();
        for (StorageDataFile dataFile : workingSubset.getDataFiles()) {
            if (dataFile.isQuicklook()) {
                try {
                    setQuicklookProperties(dataFile);
                } catch (IOException | NoSuchAlgorithmException e) {
                    // In case of error during quicklook dimensions recovering, lets set this data file to error
                    logger.error(e.getMessage(), e);
                    dataFilesNotToStore.add(dataFile);
                    progressManager.storageFailed(dataFile, "Issue occurred during quicklook dimension recovery");
                }
            }
        }
        workingSubset.getDataFiles().removeAll(dataFilesNotToStore);
        try {
            @SuppressWarnings("rawtypes")
            IDataStorage storagePlugin = pluginService.getPlugin(confIdToUse);
            storagePlugin.store(workingSubset, replaceMode, progressManager);
        } catch (ModuleException e) {
            // throwing new runtime allows us to make the job fail.
            throw new JobRuntimeException(e);
        }
    }

    private void setQuicklookProperties(StorageDataFile storageDataFile) throws IOException, NoSuchAlgorithmException {
        // first to get the quicklook properties(height and width), we need to download it.
        // unless it is already on filesystem
        Optional<URL> dataFileUrlOpt = storageDataFile.getUrls().stream()
                .filter(url -> url.getProtocol().equals("file")).findAny();
        if (!dataFileUrlOpt.isPresent()) {
            Path destination = Paths.get(getWorkspace().toString(), storageDataFile.getName());
            URL newURL = new URL("file", "", destination.toString());
            boolean downloadOk = DownloadUtils.downloadAndCheckChecksum(storageDataFile.getUrls().iterator().next(),
                                                                        destination, storageDataFile.getAlgorithm(),
                                                                        storageDataFile.getChecksum());
            if (!downloadOk) {
                String errorMsg = "Download of distant quicklook failed, dimensions cannot be set.";
                logger.error(errorMsg);
                throw new IOException(errorMsg);
            }
            storageDataFile.setUrls(Sets.newHashSet(newURL));
        }
        // then we can get the dimensions
        Path filePath = Paths.get(storageDataFile.getUrls().stream().filter(url -> url.getProtocol().equals("file"))
                .findAny().get().getPath());
        Dimension dimension = CommonFileUtils.getImageDimension(filePath.toFile());
        storageDataFile.setHeight(((Number) dimension.getHeight()).intValue());
        storageDataFile.setWidth(((Number) dimension.getWidth()).intValue());
    }

    @Override
    public int getCompletionCount() {
        return ((IWorkingSubset) parameters.get(WORKING_SUB_SET_PARAMETER_NAME).getValue()).getDataFiles().size();
    }

    @Override
    public boolean needWorkspace() {
        return true;
    }

    /**
     * @return the progress manager
     */
    public StorageJobProgressManager getProgressManager() {
        return progressManager;
    }
}
