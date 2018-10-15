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
package fr.cnes.regards.modules.acquisition.plugins.sip;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.exception.MetadataException;
import fr.cnes.regards.modules.acquisition.plugins.ISipGenerationPlugin;
import fr.cnes.regards.modules.acquisition.plugins.Microscope;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;

/**
 * BDS product SIP generation
 * @author Olivier Rousselot
 */
@Plugin(id = "BdsSipGenerationPlugin", version = "1.0.0-SNAPSHOT",
        description = "BDS product SIP generation plugin", author = "REGARDS Team", contact = "regards@c-s.fr",
        licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class BdsSipGenerationPlugin implements ISipGenerationPlugin {

    @Override
    public SIP generate(Product product) throws ModuleException {
        // Init the builder
        SIPBuilder sipBuilder = new SIPBuilder(product.getProductName());

        // Acquisition file is a .tar.gz, there should be only one file (in fact, there MUST be if we are
        // here so no need to test it)
        AcquisitionFile af = product.getActiveAcquisitionFiles().get(0);

        sipBuilder.getContentInformationBuilder()
                .setDataObject(DataType.RAWDATA, af.getFilePath().toAbsolutePath(), af.getChecksumAlgorithm(),
                               af.getChecksum());
        sipBuilder.getContentInformationBuilder().setSyntax(Microscope.GZIP_MIME_TYPE);
        sipBuilder.addContentInformation();

        // Add creation event
        sipBuilder.addEvent("Product SIP generation");

        // Add Session descriptive info (from _MD5.txt file)
        // _MD5.txt file
        File infoFile = new File(af.getFilePath().getParent().toFile(), af.getFilePath().getFileName().toString()
                .replace(Microscope.TAR_GZ_EXT, Microscope.CHECKSUM_FILE_SUFFIX));
        try {
            for (String line : Files.readAllLines(infoFile.toPath())) {
                String key = null;
                if (line.startsWith(Microscope.START_DATE_COLON)) {
                    key = Microscope.START_DATE_COLON;
                    sipBuilder.addDescriptiveInformation(Microscope.START_DATE, line.substring(key.length() + 1).trim());
                } else if (line.startsWith(Microscope.END_DATE_COLON)) {
                    key = Microscope.END_DATE_COLON;
                    sipBuilder.addDescriptiveInformation(Microscope.END_DATE, line.substring(key.length() + 1).trim());
                } else if (line.startsWith(Microscope.VERSION_COLON)) {
                    key = Microscope.VERSION_COLON;
                    sipBuilder.addDescriptiveInformation(Microscope.VERSION, line.substring(key.length() + 1).trim());
                }
            }
            return sipBuilder.build();
        } catch (IOException e) {
            throw new MetadataException(
                    String.format("Error while attempting to read file '%s'", infoFile.toString()), e);
        }
    }
}
