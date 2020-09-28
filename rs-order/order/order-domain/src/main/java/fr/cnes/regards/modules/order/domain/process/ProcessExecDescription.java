package fr.cnes.regards.modules.order.domain.process;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProcessExecDescription extends ProcessBatchDescription {

    private final List<String> inputDataFiles;

    public ProcessExecDescription(
            UUID processBusinessId,
            Map<String, String> parameters,
            UUID batchId,
            List<String> inputDataFiles
    ) {
        super(processBusinessId, parameters, batchId);
        this.inputDataFiles = inputDataFiles;
    }

    public ProcessExecDescription(ProcessBatchDescription desc, List<String> inputDataFiles) {
        super(desc, desc.getBatchId());
        this.inputDataFiles = inputDataFiles;
    }

    public List<String> getInputDataFiles() {
        return inputDataFiles;
    }
}
