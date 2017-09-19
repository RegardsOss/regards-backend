package fr.cnes.regards.modules.order.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.OrderDataFile;

/**
 * @author oroussel
 */
public interface IOrderDataFileRepository extends JpaRepository<OrderDataFile, Long> {
    /**
     * Find all available OrderDataFiles for an order.
     * An OrderDataFile 'available' is an order with 'AVAILABLE' or 'ONLINE' state (not 'DOWNLOADED')
     */
    default List<OrderDataFile> findAllAvailables(Long orderId) {
        return findByOrderIdAndStateIn(orderId, FileState.AVAILABLE, FileState.ONLINE);
    }

    List<OrderDataFile> findByOrderIdAndStateIn(Long orderId, FileState... states);

    Optional<OrderDataFile> findFirstByChecksumAndIpIdAndOrderId(String checksum, UniformResourceName aipId, Long orderId);
}
;