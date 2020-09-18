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

        String PROCESS_BY_DATASETS_PATH = PROCESS_PATH + "/fordatasets";
        String PROCESS_LINKDATASET_PATH = PROCESS_PATH + "/linkprocessdataset/{" + DATASET_PARAM + "}";

        String PROCESS_CONFIG_PATH = PROCESS_PATH + "/config";
        String PROCESS_CONFIG_BID_PATH = PROCESS_CONFIG_PATH + "/{" + PROCESS_BUSINESS_ID_PARAM + "}" ;

        String PROCESS_CONFIG_BID_USERROLE_PATH = PROCESS_CONFIG_BID_PATH + "/userRole";


        String PROCESS_METADATA_PATH = PROCESS_PATH + "/metadata";

        String MONITORING_EXECUTIONS_PATH = APIV1 + "/monitoring/executions";

    }

}
