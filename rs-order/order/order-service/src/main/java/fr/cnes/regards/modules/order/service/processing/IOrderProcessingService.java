package fr.cnes.regards.modules.order.service.processing;

import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.service.utils.OrderCounts;

public interface IOrderProcessingService {

    OrderCounts manageProcessedDatasetSelection(Order order, BasketDatasetSelection dsSel, String tenant, String user, String userRole, OrderCounts counts);
}
