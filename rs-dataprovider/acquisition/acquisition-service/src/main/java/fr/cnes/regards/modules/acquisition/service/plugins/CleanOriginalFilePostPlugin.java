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

package fr.cnes.regards.modules.acquisition.service.plugins;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.plugins.IPostProcessSipPlugin;

/**
 * A {@link Plugin} of type {@link IPostProcessSipPlugin}.<br>
 * The original file is deleted from the input folder.<br>
 * An acknowledgement of succesful completion of SIP saved by the ingest microservice can be created.<br>
 * For this three optional parameters can be used:<br>
 * <li> createAck : <code>true</code> if the acknowledgement should be created. The default value is <b><code>false</code></b>.
 * <li> folderAck : the sub folder where the acknowledgement is created. The default value is <b>ack_regards</b>.
 * <li> extensionAck : the extension added to the data file to create the acknowledgement file. The default value is <b>.regards</b>. 
 * 
 *
 * @author Christophe Mertz
 */
@Plugin(id = "CleanOriginalFilePostPlugin", version = "1.0.0-SNAPSHOT", description = "CleanOriginalFilePostPlugin",
        author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class CleanOriginalFilePostPlugin implements IPostProcessSipPlugin {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CleanOriginalFilePostPlugin.class);

    /**
     * A constant for the {@link Plugin} parameter  {@link CleanOriginalFilePostPlugin#createAck} 
     */
    public static final String CREATE_ACK_PARAM = "createAck";

    /**
     * A constant for the {@link Plugin} parameter {@link CleanOriginalFilePostPlugin#folderAck}
     */
    public static final String FOLDER_ACK_PARAM = "folderAck";

    /**
     * A constant for the {@link Plugin} parameter {@link CleanOriginalFilePostPlugin#extensionAck}
     */
    public static final String EXTENSION_ACK_PARAM = "extensionAck";

    @PluginParameter(label = "An acknowledgement of succesful completion of SIP saved by the ingest microservice",
            defaultValue = "false", optional = true)
    public Boolean createAck;

    @PluginParameter(label = "The sub folder where the acknowledgement is created", defaultValue = "ack_regards",
            optional = true)
    public String folderAck;

    @PluginParameter(label = "The extension added to the data file to create the acknowledgement file",
            defaultValue = ".regards", optional = true)
    public String extensionAck;

    @Override
    public void runPlugin(Product product, AcquisitionProcessingChain chain) throws ModuleException {
        LOGGER.info("[{}] Start post processing for the product : {}", chain.getSession(), product.getProductName());

        if (product.getMetaProduct().getCleanOriginalFile()) {
            for (AcquisitionFile acqFile : product.getAcquisitionFile()) {

                createAck(acqFile);

                acqFile.getFile().delete();
                LOGGER.debug("[{}] The file {} has been deleted", chain.getSession(), acqFile.getFileName());
            }
        }

        LOGGER.info("[{}] End  post processing for the product : {}", chain.getSession(), product.getProductName());
    }

    /**
     * Create the acknowledgement for an {@link AcquisitionFile}
     * @param acqFile the current {@link AcquisitionFile}
     */
    private void createAck(AcquisitionFile acqFile) {
        if (!createAck) {
            return;
        }

        final File acqRepository = new File(acqFile.getFile().getParentFile(), folderAck);
        final File ackFile = new File(acqRepository, acqFile.getFileName() + extensionAck);

        if (!ackFile.getParentFile().exists()) {
            ackFile.mkdirs();
        }
        try {
            if (!ackFile.createNewFile()) {
                ackFile.setLastModified(OffsetDateTime.now().toInstant().getEpochSecond());
            }
        } catch (IOException e) {
            LOGGER.error("Unable to create acknowledgement file", e);
        }

    }

}
