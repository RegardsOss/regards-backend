package fr.cnes.regards.modules.processing;

public interface ProcessingConstants {

    interface Path {
        String PROCESS_PATH = "/process";
        String BATCH_PATH = "/batch";

        String PROCESS_CONFIG_METADATA_PATH = "/process/config/metadata";
        String PROCESS_CONFIG_INSTANCES_PATH = "/process/config/instances";

        String MONITORING_EXECUTIONS_PATH = "/monitoring/executions";
    }

    interface ContentType {
        String APPLICATION_JSON = "application/json";
    }
}
