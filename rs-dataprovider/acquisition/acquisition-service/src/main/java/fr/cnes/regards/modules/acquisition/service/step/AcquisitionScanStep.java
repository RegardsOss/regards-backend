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

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.plugins.IAcquisitionScanPlugin;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionFileService;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionException;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionRuntimeException;

/**
 * @author Christophe Mertz
 *
 */
public class AcquisitionScanStep extends AbstractStep implements IAcquisitionScanStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionScanStep.class);

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IAcquisitionFileService acquisitionFileService;

    private AcquisitionProcessingChain acqProcessingChain;

    @Override
    public void proceedStep() throws AcquisitionRuntimeException, AcquisitionException {

        if (acqProcessingChain == null) {
            String msg = "The chain generation is mandatory";
            LOGGER.error(msg);
            throw new AcquisitionRuntimeException(msg);
        }

        // A plugin for the scan configuration is required
        if (this.acqProcessingChain.getScanAcquisitionPluginConf() == null) {
            throw new AcquisitionException(
                    "[" + this.acqProcessingChain.getLabel() + "] The required IAcquisitionScanPlugin is missing");
        }

        // Lunch the scan plugin
        try {
            // build the plugin parameters
            PluginParametersFactory factory = PluginParametersFactory.build();
            for (Map.Entry<String, String> entry : this.acqProcessingChain.getScanAcquisitionParameter().entrySet()) {
                factory.addDynamicParameter(entry.getKey(), entry.getValue());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Add parameter <{}> with value : {}", entry.getKey(), entry.getValue());
                }
            }

            // get an instance of the plugin
            IAcquisitionScanPlugin scanPlugin = pluginService
                    .getPlugin(this.acqProcessingChain.getScanAcquisitionPluginConf().getId(),
                               factory.getParameters().toArray(new PluginParameter[factory.getParameters().size()]));

            // launch the plugin to get the AcquisitionFile
            // c'est le plugin qui met la Date d'acquisition du fichier
            // c'est plugin qui calcule le checksum si c'est configuré dans la chaine
            Set<AcquisitionFile> acquisitionFiles = scanPlugin
                    .getAcquisitionFiles(this.acqProcessingChain.getLabel(), this.acqProcessingChain.getMetaProduct(),
                                         this.acqProcessingChain.getLastDateActivation());

            acquisitionFileService.saveAcqFilesAndChain(acquisitionFiles, acqProcessingChain);

            reportBadFiles(scanPlugin.getBadFiles(this.acqProcessingChain.getLabel(),
                                                  this.acqProcessingChain.getMetaProduct().getMetaFiles()));
        } catch (ModuleException e) {
            LOGGER.error(e.getMessage(), e);
        }

    }

    private void reportBadFiles(Set<File> badFiles) {
        if ((badFiles == null) || badFiles.isEmpty()) {
            return;
        }
        badFiles.forEach(f -> LOGGER.info("Unexpected file <{}> for the chain <{}>", f.getAbsoluteFile(),
                                          acqProcessingChain.getLabel()));
    }

    @Override
    public void getResources() throws AcquisitionException {
        this.acqProcessingChain = process.getChainGeneration();
    }

    @Override
    public void freeResources() throws AcquisitionException { // NOSONAR
    }

    @Override
    public void stop() { // NOSONAR
    }

}
