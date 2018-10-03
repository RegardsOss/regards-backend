/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.service.plugins.cdpp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.plugins.ISipGenerationPlugin;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;

/**
 * @author Marc Sordi
 *
 */
@Plugin(id = "Arcad3IsoprobeDensiteSIPGenerationPlugin", version = "1.0.0-SNAPSHOT",
        description = "Generate SIP using product information", author = "REGARDS Team", contact = "regards@c-s.fr",
        licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class Arcad3IsoprobeDensiteSIPGenerationPlugin implements ISipGenerationPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(Arcad3IsoprobeDensiteSIPGenerationPlugin.class);

    private static final String LINE_PATTERN = ".*([0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}Z).*";

    private static final Pattern linePattern = Pattern.compile(LINE_PATTERN);

    @PluginParameter(label = "Tags",
            description = "List of tags useful to classify SIP for business purpose and harvesting", optional = true)
    private List<String> tags;

    @Override
    public SIP generate(Product product) throws ModuleException {

        // Init the builder
        SIPBuilder sipBuilder = new SIPBuilder(product.getProductName());
        // Add product name to descriptive information
        sipBuilder.addDescriptiveInformation("productName", product.getProductName());

        // Fill SIP with product information
        for (AcquisitionFile af : product.getActiveAcquisitionFiles()) {
            try {
                sipBuilder.getContentInformationBuilder().setDataObject(af.getFileInfo().getDataType(),
                                                                        af.getFilePath().toAbsolutePath(),
                                                                        af.getChecksumAlgorithm(), af.getChecksum());
                sipBuilder.getContentInformationBuilder().setSyntax(af.getFileInfo().getMimeType());
                sipBuilder.addContentInformation();

                // Add descriptive information from raw data
                if (DataType.RAWDATA.equals(af.getFileInfo().getDataType())) {
                    Path rawData = af.getFilePath();
                    List<String> lines = Files.readAllLines(rawData);
                    LOGGER.trace("First line : " + lines.get(0));
                    String startDate, stopDate;

                    // Extract start date from first line
                    Matcher m = linePattern.matcher(lines.get(0));
                    if (m.matches()) {
                        startDate = m.group(1);
                    } else {
                        throw new EntityInvalidException(
                                "Cannot compute start date for file " + rawData.toAbsolutePath().toString());
                    }

                    // Extract stop date from last line
                    m = linePattern.matcher(lines.get(lines.size() - 1));
                    if (m.matches()) {
                        stopDate = m.group(1);
                    } else {
                        throw new EntityInvalidException(
                                "Cannot compute stop date for file " + rawData.toAbsolutePath().toString());
                    }

                    // Add time period
                    JsonObject timePeriod = new JsonObject();
                    timePeriod.addProperty("start_date", startDate);
                    timePeriod.addProperty("stop_date", stopDate);
                    sipBuilder.addDescriptiveInformation("time_period", timePeriod);

                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
                throw new EntityInvalidException(e.getMessage());
            }
        }

        // Add tags
        if (tags != null && !tags.isEmpty()) {
            sipBuilder.addTags(tags.toArray(new String[tags.size()]));
        }

        // Add creation event
        sipBuilder.addEvent("Product SIP generation");

        return sipBuilder.build();
    }

}
