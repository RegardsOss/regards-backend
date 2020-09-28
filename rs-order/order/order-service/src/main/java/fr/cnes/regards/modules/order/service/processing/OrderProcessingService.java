package fr.cnes.regards.modules.order.service.processing;

import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.process.ProcessDatasetDescription;
import fr.cnes.regards.modules.processing.client.IProcessingRestClient;
import fr.cnes.regards.modules.processing.domain.dto.PBatchRequest;

public class OrderProcessingService implements IOrderProcessingService {

    private final IProcessingRestClient processingClient;

    public OrderProcessingService(IProcessingRestClient processingClient) {
        this.processingClient = processingClient;
    }

    @Override
    public DatasetTask createDatasetTask(BasketDatasetSelection dsSel, ProcessDatasetDescription processDatasetDesc, String tenant, String user, String userRole) {
        PBatchRequest request = new PBatchRequest(
            "" + dsSel.getId(),
            processDatasetDesc.getProcessBusinessId().toString(),
            tenant, user, userRole,
            processDatasetDesc.getParameters(),

                dsSel.

        );
        return processingClient.createBatch(request);
    }
}
