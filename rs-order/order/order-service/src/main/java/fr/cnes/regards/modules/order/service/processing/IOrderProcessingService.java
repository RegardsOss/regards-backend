package fr.cnes.regards.modules.order.service.processing;

import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.process.ProcessDatasetDescription;

public interface IOrderProcessingService {

    DatasetTask createDatasetTask(BasketDatasetSelection dsSel, ProcessDatasetDescription processDatasetDesc);
}
