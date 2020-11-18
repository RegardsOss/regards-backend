package fr.cnes.regards.modules.processing;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.DATASET_PARAM;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.PROCESS_BUSINESS_ID_PARAM;

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
        }

        String APIV1 = "/api/v1";

        String PROCESS_PATH = APIV1 + "/process";
        String BATCH_PATH = APIV1 + "/batch";

        String BY_DATASETS_SUFFIX = "/fordatasets";
        String LINKDATASET_SUFFIX = "/linkprocessdataset/{" + DATASET_PARAM + "}";
        String CONFIG_SUFFIX = "/config";
        String CONFIG_BID_SUFFIX = CONFIG_SUFFIX + "/{" + PROCESS_BUSINESS_ID_PARAM + "}";
        String CONFIG_BID_USERROLE_SUFFIX = CONFIG_BID_SUFFIX + "/userRole";
        String METADATA_SUFFIX = "/metadata";

        String PROCESS_BY_DATASETS_PATH = PROCESS_PATH + BY_DATASETS_SUFFIX;
        String PROCESS_LINKDATASET_PATH = PROCESS_PATH + LINKDATASET_SUFFIX;
        String PROCESS_CONFIG_PATH = PROCESS_PATH + CONFIG_SUFFIX;
        String PROCESS_CONFIG_BID_PATH = PROCESS_PATH + CONFIG_BID_SUFFIX;
        String PROCESS_CONFIG_BID_USERROLE_PATH = PROCESS_PATH + CONFIG_BID_USERROLE_SUFFIX;
        String PROCESS_METADATA_PATH = PROCESS_PATH + METADATA_SUFFIX;

        String MONITORING_EXECUTIONS_PATH = APIV1 + "/monitoring/executions";

    }

}
