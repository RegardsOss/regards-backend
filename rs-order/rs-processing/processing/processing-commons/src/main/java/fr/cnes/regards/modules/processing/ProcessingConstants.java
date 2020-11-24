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

}
