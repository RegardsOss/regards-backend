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

package fr.cnes.regards.modules.acquisition.job;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.job.ChainGenerationJobParameter;
import fr.cnes.regards.modules.acquisition.plugins.IAcquisitionScanPlugin;

/**
 * @author Christophe Mertz
 *
 */
public class ScanJob extends AbstractJob<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScanJob.class);

    @Autowired
    IPluginService pluginService;

    @Override
    public void run() {
        Set<JobParameter> chains = getParameters();
        ChainGeneration chainGeneration = chains.iterator().next().getValue();

        // The MetaProduct is required
        if (chainGeneration.getMetaProduct() == null) {
            throw new RuntimeException(
                    "The required MetaProduct is missing for the ChainGeneration <" + chainGeneration.getLabel() + ">");
        }

        // A plugin for the scan configuration is required
        if (chainGeneration.getScanAcquisitionPluginConf() == null) {
            throw new RuntimeException("The required IAcquisitionScanPlugin is missing for the ChainGeneration <"
                    + chainGeneration.getLabel() + ">");
        }

        // Lunch the scan plugin
        try {
            // build the plugin parameters
            PluginParametersFactory factory = PluginParametersFactory.build();
            for (Map.Entry<String, String> entry : chainGeneration.getScanAcquisitionParameter().entrySet()) {
                factory.addParameterDynamic(entry.getKey(), entry.getValue());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Add <" + entry.getKey() + "> parameter " + entry.getValue() + " : ");
                }
            }

            // get an instance of the plugin
            IAcquisitionScanPlugin scanPlugin = pluginService
                    .getPlugin(chainGeneration.getScanAcquisitionPluginConf(),
                               factory.getParameters().toArray(new PluginParameter[factory.getParameters().size()]));

            // launch the plugin to get the AcquisitionFile
            Set<AcquisitionFile> acquistionFiles = scanPlugin.getAcquisitionFiles();

            acquistionFiles.size();

            // question : quand persister en base
            // il faut logBook
            // attention à la reprise

        } catch (ModuleException e) {
            LOGGER.error(e.getMessage(), e);
        }

    }

    @Override
    public void setParameters(Set<JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        if (parameters.isEmpty()) {
            throw new JobParameterMissingException("No parameter provided");
        }
        if (parameters.size() != 1) {
            throw new JobParameterInvalidException("Only one parameter is expected.");
        }
        JobParameter param = parameters.iterator().next();
        if (!ChainGenerationJobParameter.isCompatible(param)) {
            throw new JobParameterInvalidException(
                    "Please use ChainGenerationJobParameter in place of JobParameter (this "
                            + "class is here to facilitate your life so please use it.");
        }
        super.parameters = parameters;
    }

}
