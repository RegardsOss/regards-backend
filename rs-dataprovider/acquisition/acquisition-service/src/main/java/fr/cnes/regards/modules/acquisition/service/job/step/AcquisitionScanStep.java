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
package fr.cnes.regards.modules.acquisition.service.job.step;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.step.ProcessingStepException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.plugins.IAcquisitionScanPlugin;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionFileService;
import fr.cnes.regards.modules.acquisition.service.job.ProductAcquisitionJob;

/**
 * Acquisition scan step
 * @author Marc Sordi
 *
 */
public class AcquisitionScanStep extends AbstractDataProviderStep<Void, Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionScanStep.class);

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IAcquisitionFileService acquisitionFileService;

    public AcquisitionScanStep(ProductAcquisitionJob job) {
        super(job);
    }

    @Override
    protected Void doExecute(Void in) throws ProcessingStepException {

        LOGGER.debug("[{}] : acquistion scan step started.", acqProcessingChain.getLabel());

        // Launch the scan plugin
        try {
            // build the plugin parameters
            PluginParametersFactory factory = PluginParametersFactory.build();
            for (Map.Entry<String, String> entry : this.acqProcessingChain.getScanAcquisitionParameter().entrySet()) {
                factory.addParameter(entry.getKey(), entry.getValue());
                LOGGER.debug("[{}] : add parameter <{}> with value : {}", acqProcessingChain.getLabel(), entry.getKey(),
                             entry.getValue());
            }

            // get an instance of the plugin
            IAcquisitionScanPlugin scanPlugin = pluginService
                    .getPlugin(acqProcessingChain.getScanAcquisitionPluginConf().getId(), factory.asArray());

            // launch the plugin to get the AcquisitionFile
            Set<AcquisitionFile> acquisitionFiles = scanPlugin
                    .getAcquisitionFiles(acqProcessingChain.getLabel(), acqProcessingChain.getMetaProduct(),
                                         acqProcessingChain.getLastDateActivation());

            acquisitionFileService.saveAcqFilesAndChain(acquisitionFiles, acqProcessingChain);

            reportBadFiles(scanPlugin.getBadFiles(this.acqProcessingChain.getLabel(),
                                                  this.acqProcessingChain.getMetaProduct().getMetaFiles()));
        } catch (ModuleException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    private void reportBadFiles(Set<File> badFiles) {
        if ((badFiles != null)) {
            badFiles.forEach(f -> LOGGER.debug("[{}] : unexpected file {}.", acqProcessingChain.getLabel(),
                                               f.getAbsoluteFile()));
        }
    }
}
