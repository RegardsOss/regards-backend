package fr.cnes.regards.modules.processing;

public interface ProcessingConstants {

    interface Path {
        interface Param {
            String PROCESS_BUSINESS_ID_PARAM = "processBusinessId";

            String TENANT_PARAM = "tenant";
            String STATUS_PARAM = "status";

            String PAGE_PARAM = "page";
            String SIZE_PARAM = "size";

            String USER_EMAIL_PARAM = "email";
            String DATE_FROM_PARAM = "from";
            String DATE_TO_PARAM = "to";
        }

        String APIV1 = "/api/v1";

        String PROCESS_PATH = APIV1 + "/process";
        String BATCH_PATH = APIV1 + "/batch";

        String PROCESS_CONFIG_PATH = PROCESS_PATH + "/config";
        String PROCESS_CONFIG_BID_PATH = PROCESS_CONFIG_PATH + "/{" + Param.PROCESS_BUSINESS_ID_PARAM + "}" ;

        String PROCESS_METADATA_PATH = PROCESS_PATH + "/metadata";

        String MONITORING_EXECUTIONS_PATH = APIV1 + "/monitoring/executions";

        static String param(String name) { return "{" + name + "}"; }
    }

}
