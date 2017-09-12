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
package fr.cnes.regards.modules.ingest.domain;

import fr.cnes.regards.framework.urn.DataType;

/**
 * Builder for {@link SIP}
 *
 * @author Marc Sordi
 *
 */
public class SIPBuilder {

    private final SIP sip = new SIP();

    public SIP build() {
        return sip;
    }

    /**
     * Add a file reference to the SIP
     *
     * @param url URL of the file
     * @param mimeType MIME type
     * @param dataType {@link DataType}
     * @param fileSize optional file size
     * @param checksum checksum
     * @param checksumAlgorithm checksum algorithm
     */
    public void addDataObject(String url, String mimeType, DataType dataType, Long fileSize, String checksum,
            String checksumAlgorithm) {
        SIPDataObject dataObject = new SIPDataObject();
        dataObject.setUrl(url);
        dataObject.setMimeType(mimeType);
        dataObject.setDataType(dataType);
        dataObject.setFileSize(fileSize);
        dataObject.setChecksum(checksum);
        dataObject.setChecksumAlgorithm(checksumAlgorithm);
        sip.addDataObject(dataObject);
    }

    public void addDataObject(String url, String mimeType, DataType dataType, String checksum,
            String checksumAlgorithm) {
        addDataObject(url, mimeType, dataType, null, checksum, checksumAlgorithm);
    }
}
