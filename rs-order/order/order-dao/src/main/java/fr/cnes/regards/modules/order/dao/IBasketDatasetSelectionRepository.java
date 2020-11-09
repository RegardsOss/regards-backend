package fr.cnes.regards.modules.order.dao;

import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IBasketDatasetSelectionRepository  extends JpaRepository<BasketDatasetSelection, Long> {

    @Query(
        value = "SELECT BDS.*" +
                " FROM t_basket_dataset AS BDS" +
                " WHERE process_dataset_desc->>'processBusinessId' = :processBusinessId",
            nativeQuery = true
    )
    List<BasketDatasetSelection> findByProcessBusinessId(
            @Param("processBusinessId") String processBusinessId
    );

}
