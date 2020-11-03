package fr.cnes.regards.modules.order.dao;

import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IBasketDatasetSelectionRepository  extends JpaRepository<BasketDatasetSelection, Long> {
}
