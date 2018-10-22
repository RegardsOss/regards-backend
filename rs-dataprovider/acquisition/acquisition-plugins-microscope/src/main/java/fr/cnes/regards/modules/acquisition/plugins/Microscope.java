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

    public static final String START_DATE_COLON = "StartDate:";

    public static final String END_DATE_COLON = "EndDate:";

    public static final String VERSION_COLON = "Version:";

    public static final String CHECKSUM_FILE_SUFFIX = "_MD5.txt";

    public static final String TGZ_EXT = ".tgz";

    public static final String TAR_GZ_EXT = ".tar.gz";

    public static final String ZIP_EXT = ".zip";

    public static final String NCD_EXT = ".nc";

    // SIP Descriptive informations keys
    public static final String START_DATE = "StartDate";

    public static final String END_DATE = "EndDate";

    public static final String STATION = "Station";

    public static final String APID = "APID";

    public static final String APID_MNEMONIC = "APID_Mnémonique";

    public static final String TM_TYPE = "Type de TM";

    public static final String BDS_VERSION = "Version BDS";

    public static final String VERSION = "Version";

    public static final String SCOPE = "Scope";

    public static final String SESSION = "Session";

    public static final String SESSION_NB = "NumSession";

    public static final String PHASE = "Phase";

    public static final String TECHNO = "Techno";

    public static final String SESSION_TYPE = "SessionType";

    public static final String SESSION_SUB_TYPE = "SessionSubType";

    public static final String SPIN = "SPIN";

    public static final String CAL_PARAM = "CalParam";

    public static final String RECORD_FILE_NAME = "RecordFileName";

    public static final String RECORD_VERSION = "RecordVersion";

    public static final String ENV_CONSTRAINT = "EnvConstraint";

    public static final String ACTIVE_SU = "ActiveSU";

    public static final String PID_VERSION = "PIDVersion";

    public static final String ORBITS_COUNT = "NumberOfOrbits";

    public static final String COMMENT = "Comment";

    // NETCDF TAGS
    public static final String SESSION_NB_NC_TAG = SESSION_NB;

    public static final String START_DATE_NC_TAG = START_DATE;

    public static final String END_DATE_NC_TAG = END_DATE;

    public static final String PHASE_NC_TAG = PHASE;

    public static final String TECHNO_NC_TAG = TECHNO;

    public static final String SESSION_TYPE_NC_TAG = SESSION_TYPE;

    public static final String SESSION_SUB_TYPE_NC_TAG = SESSION_SUB_TYPE;

    public static final String ROTATE_MOD_NC_TAG = "RotateMod";

    public static final String CAL_PARAM_NC_TAG = CAL_PARAM;

    public static final String RECORD_FILE_NAME_NC_TAG = RECORD_FILE_NAME;

    public static final String RECORD_VERSION_NC_TAG = RECORD_VERSION;

    public static final String ENV_CONSTRAINT_NC_TAG = ENV_CONSTRAINT;

    public static final String ACTIVE_SU_NC_TAG = ACTIVE_SU;

    public static final String PID_VERSION_NC_TAG = PID_VERSION;

    public static final String ORBITS_COUNT_NC_TAG = ORBITS_COUNT;

    public static final String COMMENT_NC_TAG = COMMENT;

    // Missing MimeTypes
    public static final MimeType GZIP_MIME_TYPE = new MimeType("application", "gzip");

    public static final MimeType TSV_MIME_TYPE = new MimeType("text", "tab-separated-values");

    private Microscope() {
    }
}
