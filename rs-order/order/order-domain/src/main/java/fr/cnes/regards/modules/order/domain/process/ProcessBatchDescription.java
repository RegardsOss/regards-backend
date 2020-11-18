package fr.cnes.regards.modules.order.domain.process;

import java.util.Map;
import java.util.UUID;

public class ProcessBatchDescription extends ProcessDatasetDescription {

    private final UUID batchId;

    public ProcessBatchDescription(UUID processBusinessId, Map<String, String> parameters, UUID batchId) {
        super(processBusinessId, parameters);
        this.batchId = batchId;
    }

    public ProcessBatchDescription(ProcessDatasetDescription desc, UUID batchId) {
        super(desc.getProcessBusinessId(), desc.getParameters());
        this.batchId = batchId;
    }

    public UUID getBatchId() {
        return batchId;
    }
}
