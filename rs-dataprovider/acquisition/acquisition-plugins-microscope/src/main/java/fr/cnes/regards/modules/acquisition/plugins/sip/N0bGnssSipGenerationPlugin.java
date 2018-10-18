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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.springframework.util.MimeType;
import org.w3c.dom.Document;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.acquisition.exception.MetadataException;
import fr.cnes.regards.modules.acquisition.plugins.MicroHelper;
import fr.cnes.regards.modules.acquisition.plugins.Microscope;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;

/**
 * N0b_GNSS product SIP generation based upon FromMetadataSipGenerationPlugin
 * @author Olivier Rousselot
 */
@Plugin(id = "N0bGnssSipGenerationPlugin", version = "1.0.0-SNAPSHOT",
        description = "N0b_GNSS product SIP generation plugin", author = "REGARDS Team", contact = "regards@c-s.fr",
        licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class N0bGnssSipGenerationPlugin extends FromMetadataSipGenerationPlugin {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat(Microscope.DATE_FORMAT_PATTERN);

    @Override
    protected String getDataFileChecksum(Document doc, File dataFile) throws IOException, NoSuchAlgorithmException {
        return ChecksumUtils.computeHexChecksum(new FileInputStream(dataFile), Microscope.CHECKSUM_ALGO);
    }

    @Override
    protected File findDataFile(Path metadataFilePath, String filename) throws IOException {
        return MicroHelper.findFileWithExtension(metadataFilePath.toFile().getParentFile(), Microscope.TGZ_EXT);
    }

    @Override
    protected void addDescriptiveInformations(SIPBuilder sipBuilder, Document doc) throws MetadataException {
        String startDate = MicroHelper.getTagValue(doc, Microscope.START_DATE_TAG);
        sipBuilder.addDescriptiveInformation(Microscope.START_DATE, startDate);
        try {
            Date endDate = dateFormat.parse(startDate);
            // Add 24 hours
            endDate.setTime(endDate.toInstant().plus(24, ChronoUnit.HOURS).toEpochMilli());
            sipBuilder.addDescriptiveInformation(Microscope.END_DATE, dateFormat.format(endDate));
        } catch (ParseException e) {
            throw new MetadataException(
                    String.format("Cannot interpret date '%s' from '%s' tag value with format '%s'", startDate,
                                  Microscope.START_DATE_TAG, Microscope.DATE_FORMAT_PATTERN), e);
        }
    }

    @Override
    protected MimeType getDataFileMimeType() {
        return Microscope.GZIP_MIME_TYPE;
    }

}
