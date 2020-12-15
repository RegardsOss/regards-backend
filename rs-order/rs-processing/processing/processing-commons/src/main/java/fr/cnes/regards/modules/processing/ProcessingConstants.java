/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.DATASET_PARAM;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.PROCESS_BUSINESS_ID_PARAM;

/**
 * Various constants (paths, parameter names, etc.) used in different modules.
 *
 * @author gandrieu
 */
public interface ProcessingConstants {

    interface Path {

        interface Param {

            String PROCESS_BUSINESS_ID_PARAM = "processBusinessId";

            String TENANT_PARAM = "tenant";

            String STATUS_PARAM = "status";

            String PAGE_PARAM = "page";

            String SIZE_PARAM = "size";

            String USER_EMAIL_PARAM = "userEmail";

            String USER_ROLE_PARAM = "userRole";

            String DATE_FROM_PARAM = "from";

            String DATE_TO_PARAM = "to";

            String DATASET_PARAM = "datasetIpId";

            String PROCESS_BID_PARAM = "processBusinessId";
        }

        String PROCESS_PATH = "/process";

        String PROCESSPLUGIN_PATH = "/processplugins";

        String BATCH_PATH = "/batch";

        String BY_DATASETS_SUFFIX = "/fordatasets";

        String LINKDATASET_SUFFIX = "/linkprocessdataset/{" + DATASET_PARAM + "}";

        String CONFIG_SUFFIX = "/config";

        String BID_SUFFIX = "/{" + PROCESS_BUSINESS_ID_PARAM + "}";

        String BID_USERROLE_SUFFIX = BID_SUFFIX + "/userRole";

        String CONFIG_BID_SUFFIX = CONFIG_SUFFIX + BID_SUFFIX;

        String CONFIG_BID_USERROLE_SUFFIX = CONFIG_SUFFIX + BID_USERROLE_SUFFIX;

        String METADATA_SUFFIX = "/metadata";

        String MONITORING_EXECUTIONS_PATH = "/monitoring/executions";

    }

    interface Engines {

        String JOBS = "JOBS";
    }

}
