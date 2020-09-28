package fr.cnes.regards.modules.order.domain.process;

import java.util.Map;
import java.util.UUID;

public class ProcessDatasetDescription {

    private final UUID processBusinessId;
    private final Map<String, String> parameters;

    public ProcessDatasetDescription(UUID processBusinessId, Map<String, String> parameters) {
        this.processBusinessId = processBusinessId;
        this.parameters = parameters;
    }

    public UUID getProcessBusinessId() {
        return processBusinessId;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

}
