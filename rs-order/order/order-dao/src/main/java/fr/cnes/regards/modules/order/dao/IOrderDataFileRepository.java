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

    @Modifying
    @Query("update OrderDataFile o set o.state = ?1 where o.checksum = ?2 and o.ipId = ?2 and o.orderId = ?3")
    int setStateForAipChecksumAndOrderId(FileState state, String checksum, UniformResourceName aipId, Long orderId);

    @Query(nativeQuery = true,
            value = "SELECT * FROM t_data_file f WHERE f.state IN ('AVAILABLE', 'ONLINE') AND f.files_task_id IN (?1)")
    List<OrderDataFile> findAllAvailableAndOnline(Collection<Long> fileTaskIds);

    Optional<OrderDataFile> findFirstByChecksumAndIpIdAndOrderId(String checksum, UniformResourceName aipId, Long orderId);
}
