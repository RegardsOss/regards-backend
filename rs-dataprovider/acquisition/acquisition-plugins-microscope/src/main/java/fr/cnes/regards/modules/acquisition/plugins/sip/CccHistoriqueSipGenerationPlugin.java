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

import org.springframework.util.MimeType;
import org.w3c.dom.Document;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.acquisition.exception.MetadataException;
import fr.cnes.regards.modules.acquisition.plugins.Microscope;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;

/**
 * CCC_HISTORIQUE product SIP generation based upon FromMetadataSipGenerationPlugin
 * @author Olivier Rousselot
 */
@Plugin(id = "CccHistoriqueSipGenerationPlugin", version = "1.0.0-SNAPSHOT",
        description = "CCC_RAW_HKTM product SIP generation plugin", author = "REGARDS Team", contact = "regards@c-s.fr",
        licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class CccHistoriqueSipGenerationPlugin extends FromMetadataSipGenerationPlugin {
    private static final String LAUNCH_DATE = "25/04/2016 00:00:00.000";

    private static final String MISSION_END_DATE = "16/12/2018 00:00:00.000";

    @Override
    protected void addDescriptiveInformations(SIPBuilder sipBuilder, Document doc) throws MetadataException {
        sipBuilder.addDescriptiveInformation(Microscope.START_DATE, LAUNCH_DATE);
        sipBuilder.addDescriptiveInformation(Microscope.END_DATE, MISSION_END_DATE);
    }

    @Override
    protected MimeType getDataFileMimeType() {
        return Microscope.GZIP_MIME_TYPE;
    }
}
