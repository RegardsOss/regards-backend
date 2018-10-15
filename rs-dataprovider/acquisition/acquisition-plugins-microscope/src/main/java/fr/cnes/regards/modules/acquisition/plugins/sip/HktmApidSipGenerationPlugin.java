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
import fr.cnes.regards.modules.acquisition.plugins.MicroHelper;
import fr.cnes.regards.modules.acquisition.plugins.Microscope;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;

/**
 * HKTM_APID product SIP generation based upon CccRawHktmSipGenerationPlugin
 * @author Olivier Rousselot
 */
@Plugin(id = "DopplerSipGenerationPlugin", version = "1.0.0-SNAPSHOT",
        description = "HKTM_APID product SIP generation plugin", author = "REGARDS Team", contact = "regards@c-s.fr",
        licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class HktmApidSipGenerationPlugin extends CccRawHktmSipGenerationPlugin {

    @Override
    protected void addDescriptiveInformations(SIPBuilder sipBuilder, Document doc) throws MetadataException {
        super.addDescriptiveInformations(sipBuilder, doc);
        sipBuilder.addDescriptiveInformation(Microscope.APID_MNEMONIC, MicroHelper.getTagValue(doc, Microscope.APID_MNEMONIC_TAG));
        sipBuilder.addDescriptiveInformation(Microscope.TM_TYPE, MicroHelper.getTagValue(doc, Microscope.HKTM_APID_TYPE_TAG));
        sipBuilder.addDescriptiveInformation(Microscope.BDS_VERSION, MicroHelper.getTagValue(doc, Microscope.BDS_VERSION_TAG));
    }

    @Override
    protected MimeType getDataFileMimeType() {
        return Microscope.TSV_MIME_TYPE;
    }
}
