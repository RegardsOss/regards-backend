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
package fr.cnes.regards.modules.acquisition.plugins;

import org.springframework.util.MimeType;

/**
 * Microscope Properties class
 * @author Olivier Rousselot
 */
public final class Microscope {

    // XML metadata file related informations
    public static final String SAG_DESCRIPTOR = "sag_descripteur.xml";

    public static final String METADATA_SUFFIX = "_metadata.xml";

    public static final String HKTM_METADATA_ROOT_DIR = "metadonnees";

    public static final String DATE_FORMAT_PATTERN = "dd/MM/yyyy HH:mm:ss.SSS";

    // CHECKSUM algorithm
    public static final String CHECKSUM_ALGO = "MD5";

    // XML TAGS
    public static final String FILENAME_TAG = "nomFichierDonnee";

    public static final String CHECKSUM_TAG = "md5Check";

    public static final String START_DATE_TAG = "startDate";

    public static final String END_DATE_TAG = "endDate";

    public static final String STATION_ID_TAG = "stationId";

    public static final String APID_TAG = "apid";

    public static final String APID_MNEMONIC_TAG = "apidMnemo";

    public static final String HKTM_APID_TYPE_TAG = "hktmAPIDType";

    public static final String BDS_VERSION_TAG = "versionBDS";

    public static final String N0_TYPE_TAG = "n0Type";

    public static final String SESSION_SEQUENCE_NUMBER_TAG = "numeroSequenceSession";

    // MD5.txt related informations
    public static final String CHECKSUM_KEY_COMMENT = "# Archive MD5:";

    public static final String CHECKSUM_FILE_SUFFIX = "_MD5.txt";

    public static final String TGZ_EXT = ".tgz";

    public static final String TAR_GZ_EXT = ".tar.gz";

    // SIP Descriptive informations keys
    public static final String START_DATE = "StartDate";

    public static final String END_DATE = "EndDate";

    public static final String STATION = "Station";

    public static final String APID = "APID";

    public static final String APID_MNEMONIC = "APID_Mnémonique";

    public static final String TM_TYPE = "Type de TM";

    public static final String BDS_VERSION = "Version BDS";

    public static final String SCOPE = "Scope";

    public static final String SESSION = "Session";

    // Missing MimeTypes
    public static final MimeType GZIP_MIME_TYPE = new MimeType("application", "gzip");

    public static final MimeType TSV_MIME_TYPE = new MimeType("text", "tab-separated-values");

    private Microscope() {
    }
}
