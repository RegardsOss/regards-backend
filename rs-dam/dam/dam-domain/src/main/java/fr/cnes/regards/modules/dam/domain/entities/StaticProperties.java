/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.dam.domain.entities;

import java.util.Arrays;
import java.util.List;

/**
 * List all available static properties
 *
 * @author Marc Sordi
 */
public final class StaticProperties {

    // URN (duplicate as ID in feature)
    public static final String IP_ID = "ipId";

    // ##########-AbstractEntity-##########

    // Model
    public static final String MODEL_TYPE = "model";

    // String
    public static final String MODEL_NAME = MODEL_TYPE + ".name";

    // String
    public static final String MODEL_DESCRIPTION = MODEL_TYPE + ".description";

    // String
    public static final String MODEL_VERSION = MODEL_TYPE + ".version";

    // Last update search attribute name. To avoid concurrency with possible dynamic attribute model, prefix with _
    public static final String LAST_UPDATE = "_lastUpdate";

    // Last update path in entities.
    public static final String LAST_UPDATE_PATH = "lastUpdate";

    // Creation date search attribute name. To avoid concurrency with possible dynamic attribute model, prefix with _
    public static final String CREATION_DATE = "_creationDate";

    // Creation date path in entities.
    public static final String CREATION_DATE_PATH = "creationDate";

    // Long
    public static final String ID = "id";

    // String list
    public static final String GROUPS = "groups";

    // ##########-EntityFeature-##########

    public static final String FEATURE = "feature";

    /**
     * Feature namespace
     */
    public static final String FEATURE_NS = FEATURE + ".";

    // String
    public static final String FEATURE_ID = "id";

    public static final String FEATURE_ID_PATH = FEATURE_NS + FEATURE_ID;

    // String
    public static final String FEATURE_PROVIDER_ID = "providerId";

    public static final String FEATURE_PROVIDER_ID_PATH = FEATURE_NS + FEATURE_PROVIDER_ID;

    // String
    public static final String FEATURE_LABEL = "label";

    public static final String METADATA = "metadata";

    public static final String META_DATA_GROUPS = METADATA + "." + GROUPS;

    public static final String DATA_ACCESS_RIGHT = "dataAccessRight";

    public static final String FEATURE_LABEL_PATH = FEATURE_NS + FEATURE_LABEL;

    // String
    public static final String FEATURE_MODEL = "model";

    public static final String FEATURE_MODEL_PATH = FEATURE_NS + FEATURE_MODEL;

    // List of DataFile
    public static final String FEATURE_FILES = "files";

    public static final String FEATURE_FILES_PATH = FEATURE_NS + FEATURE_FILES;

    // String list
    public static final String FEATURE_TAGS = "tags";

    // Rawdata file name search attribute name
    public static final String FEATURE_FILE_RAWDATA_FILENAME = "rawdata";

    // Rawdata file name search attribute path
    public static final String FEATURE_FILE_RAWDATA_FILENAME_PROPERTY_PATH = FEATURE_FILES + ".RAWDATA.filename";

    public static final String FEATURE_TAGS_PATH = FEATURE_NS + FEATURE_TAGS;

    // VirtualId

    public static final String FEATURE_VIRTUAL_ID = "virtualId";

    public static final String FEATURE_VIRTUAL_ID_PATH = FEATURE_NS + FEATURE_VIRTUAL_ID;

    //Version
    public static final String FEATURE_VERSION = "version";

    public static final String FEATURE_VERSION_PATH = FEATURE_NS + FEATURE_VERSION;

    // Is last version
    public static final String FEATURE_IS_LAST_VERSION = "last";

    public static final String FEATURE_IS_LAST_VERSION_PATH = FEATURE_NS + FEATURE_IS_LAST_VERSION;

    // Geometry
    public static final String FEATURE_GEOMETRY = "geometry";

    public static final String FEATURE_GEOMETRY_PATH = FEATURE_NS + FEATURE_GEOMETRY;

    // Wrapped dynamic properties
    public static final String FEATURE_PROPERTIES = "properties";

    public static final String FEATURE_PROPERTIES_PATH = FEATURE_NS + FEATURE_PROPERTIES;

    // List of first level static properties
    public static final List<String> FEATURES_STATICS = Arrays.asList(FEATURE_ID,
                                                                      FEATURE_PROVIDER_ID,
                                                                      FEATURE_LABEL,
                                                                      FEATURE_MODEL,
                                                                      FEATURE_FILES,
                                                                      FEATURE_TAGS,
                                                                      FEATURE_GEOMETRY,
                                                                      FEATURE_PROPERTIES);

    // ##########-DataObject-##########

    public static final String DATASOURCE_ID = "dataSourceId";

    // Long list
    public static final String DATASET_MODEL_NAMES = "datasetModelNames";

    // ##########-Dataset-##########

    // String
    public static final String DATA_MODEL = "dataModel";

    private StaticProperties() {
    }
}
