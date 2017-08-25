/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service.job;

import java.net.URL;
import java.util.Map;
import java.util.Set;

import javax.validation.Validation;
import javax.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Maps;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.PluginService;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.plugin.IDataStorage;

/**
 * This job is executed by JobService while its scheduling is handled by a DataStorage plugin. This means that the job context is prepared by the plugin and not a Service.
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class StoreAipMetadataJob extends AbstractJob<AIP> {

    private static final Logger LOG = LoggerFactory.getLogger(StoreAipMetadataJob.class);

    public static final String PLUGIN_TO_USE_PARAMETER_NAME = "pluginToUse";

    public static final String AIP_PARAMETER_NAME = "aip";

    private static final String PARAMETER_MISSING = "%s requires a %s as \"%s\" parameter";

    private static final String PARAMETER_INVALID = "%s requires a valid %s(identifier: %s)";

    @Autowired
    private PluginService pluginService;

    /**
     * Check that the given job parameters contains required parameters and that they are valid.
     *
     * @return a map which keys are the job parameter name and its value the job parameter. This map contains no entry if there is no parameter provided.
     * @throws JobParameterMissingException
     * @throws JobParameterInvalidException
     */
    public static Map<String, JobParameter> checkParameters(Set<JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        //lets sort parameters by name
        Map<String, JobParameter> parametersMap = Maps.newHashMap();
        parameters.forEach(jp -> parametersMap.put(jp.getName(), jp));
        //lets see if the plugin to use has been given through a plugin configuration.
        JobParameter pluginToUse;
        if (((pluginToUse = parametersMap.get(PLUGIN_TO_USE_PARAMETER_NAME)) == null)
                || !(pluginToUse.getValue() instanceof PluginConfiguration)) {
            JobParameterMissingException e = new JobParameterMissingException(
                    String.format(PARAMETER_MISSING, StoreAipMetadataJob.class.getName(), URL.class.getName(),
                                  PLUGIN_TO_USE_PARAMETER_NAME));
            LOG.error(e.getMessage(), e);
            throw e;
        }
        //now that we are sure there is a plugin configuration as parameter, lets check if its a plugin configuration of IDataStorage
        PluginConfiguration confToUse = pluginToUse.getValue();
        if (!confToUse.getInterfaceNames().contains(IDataStorage.class.getName())) {
            JobParameterInvalidException e = new JobParameterInvalidException(
                    String.format(PARAMETER_INVALID, StoreAipMetadataJob.class.getName(),
                                  IDataStorage.class.getName() + " configuration", confToUse.getId()));
            LOG.error(e.getMessage(), e);
            throw e;
        }
        JobParameter aip;
        if (((aip = parametersMap.get(AIP_PARAMETER_NAME)) == null) || !(aip.getValue() instanceof AIP)) {
            JobParameterMissingException e = new JobParameterMissingException(
                    String.format(PARAMETER_MISSING, StoreAipMetadataJob.class.getName(), AIP.class.getName(),
                                  AIP_PARAMETER_NAME));
            LOG.error(e.getMessage(), e);
            throw e;
        }
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        AIP aipValue = aip.getValue();
        if (validator.validate(aipValue).size() != 0) {
            JobParameterInvalidException e = new JobParameterInvalidException(
                    String.format(PARAMETER_INVALID, StoreAipMetadataJob.class.getName(), AIP.class.getName(),
                                  aipValue.getIpId()));
            LOG.error(e.getMessage(), e);
            throw e;
        }
        return parametersMap;
    }

    @Override
    public void run() {
        // first lets check that all parameters are there and valid.
        Map<String, JobParameter> parameterMap;
        try {
            parameterMap = checkParameters(parameters);
        } catch (JobParameterMissingException | JobParameterInvalidException e) {
            throw new RuntimeException(e);
        }
        // now lets instantiate the plugin to use
        PluginConfiguration confToUse = parameterMap.get(PLUGIN_TO_USE_PARAMETER_NAME).getValue();
        try {
            IDataStorage storagePlugin = pluginService.getPlugin(confToUse.getId());
            // now that we have the plugin instance, lets retrieve the aip from the job parameters and ask the plugin to do the storage
            AIP aip = parameterMap.get(AIP_PARAMETER_NAME).getValue();
            // storagePlugin.storeMetadata(aip);
            // TODO new interface
        } catch (ModuleException e) {
            //throwing new runtime allows us to make the job fail.
            throw new RuntimeException(e);
        }

    }
}
