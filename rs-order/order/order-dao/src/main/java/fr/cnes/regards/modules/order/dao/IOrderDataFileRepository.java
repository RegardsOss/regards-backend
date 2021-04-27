/* Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
*/
package fr.cnes.regards.modules.order.dao;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.persistence.Convert;
import javax.persistence.Converts;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Specific OrderDataFile repository methods
 * @author oroussel
 */
@Repository
public interface IOrderDataFileRepository extends JpaRepository<OrderDataFile, Long> {

    /**
     * Find all available OrderDataFiles for an order.
     * An OrderDataFile 'available' is an order with 'AVAILABLE' state (not 'DOWNLOADED')
     */
    default List<OrderDataFile> findAllAvailables(Long orderId) {
        return findByOrderIdAndStateIn(orderId, FileState.AVAILABLE);
    }

    List<OrderDataFile> findAllByUrlStartingWith(String repr);

    List<OrderDataFile> findByOrderIdAndStateIn(Long orderId, FileState... states);

    List<OrderDataFile> findAllByOrderId(Long orderId);

    Optional<OrderDataFile> findFirstByChecksumAndIpIdAndOrderId(String checksum, UniformResourceName aipId,
            Long orderId);

    /**
     * Return a list of { Order, sum of file size (Long) } for not finished orders whom expiration date is after the one
     * provided
     */
    @Query(name = "selectSumSizesByOrderId") // Query is defined on OrderDataFile class
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    List<Object[]> findSumSizesByOrderId(OffsetDateTime limitDate);

    /**
     * Return a list of { Order, sum of file size (Long) } for notfinished orders whom expiration date is after the one
     * provided and whom files state is one of provided ones
     * @param states must not be null nor empty !!!!
     */
    @Query(name = "selectSumSizesByOrderIdAndStates") // Query is defined on OrderDataFile class
    @Converts({ @Convert(converter = OffsetDateTimeAttributeConverter.class) })
    List<Object[]> selectSumSizesByOrderIdAndStates(OffsetDateTime limitDate, Collection<String> states);

    default List<Object[]> selectSumSizesByOrderIdAndStates(OffsetDateTime limitDate, FileState... states) {
        return selectSumSizesByOrderIdAndStates(limitDate, Arrays.asList(states).stream().map(FileState::toString)
                .collect(Collectors.toList()));
    }

    /**
     * Return a list of { Order, file count (Long) } for not finished orders whom expiration date is after the one
     * provided and whom files state is one of provided ones
     * @param states must not be null nor empty !!!!
     */
    @Query(name = "selectCountFilesByOrderIdAndStates") // Query is defined on OrderDataFile class
    @Converts({ @Convert(converter = OffsetDateTimeAttributeConverter.class),
            @Convert(converter = FileStateConverter.class), @Convert(converter = FileStateCollectionConverter.class) })
    List<Object[]> selectCountFilesByOrderIdAndStates(OffsetDateTime limitDate, Collection<String> states);

    default List<Object[]> selectCountFilesByOrderIdAndStates(OffsetDateTime limitDate, FileState... states) {
        return selectCountFilesByOrderIdAndStates(limitDate, Arrays.asList(states).stream().map(FileState::toString)
                .collect(Collectors.toList()));
    }

    /**
     * Return a list of { Order, file count (Long) } for all orders and whom expiration date is after the one provided
     * and whom files state is one of provided ones
     * @param states must not be null nor empty !!!!
     */
    @Query(name = "selectCountFilesByOrderIdAndStates4AllOrders") // Query is defined on OrderDataFile class
    @Converts({ @Convert(converter = OffsetDateTimeAttributeConverter.class),
            @Convert(converter = FileStateConverter.class), @Convert(converter = FileStateCollectionConverter.class) })
    List<Object[]> selectCountFilesByOrderIdAndStates4AllOrders(OffsetDateTime limitDate, Collection<String> states);

    default List<Object[]> selectCountFilesByOrderIdAndStates4AllOrders(OffsetDateTime limitDate, FileState... states) {
        return selectCountFilesByOrderIdAndStates4AllOrders(limitDate, Arrays.asList(states).stream()
                .map(FileState::toString).collect(Collectors.toList()));
    }

    @Modifying
    void deleteByOrderId(Long orderId);

    @Query(value = "SELECT df.* " +
            "FROM {h-schema}t_data_file df " +
            "JOIN {h-schema}t_task t ON t.id = df.files_task_id " +
            "JOIN {h-schema}t_dataset_task dt ON dt.id = t.parent_id " +
            "WHERE dt.id = ?1 " +
            "AND df.data_objects_ip_id IN " +
            "(" +
            "    SELECT df.data_objects_ip_id " +
            "    FROM {h-schema}t_data_file df " +
            "    JOIN {h-schema}t_task t ON t.id = df.files_task_id " +
            "    JOIN {h-schema}t_dataset_task dt ON dt.id = t.parent_id " +
            "    WHERE dt.id = ?1 " +
            "    AND df.state IN ?2 " +
            "    LIMIT ?3 " +
            ") " +
            "ORDER BY df.id",
            nativeQuery = true)
    List<OrderDataFile> selectByDatasetTaskAndStateAndLimit(long datasetTaskId, List<String> states, int limit);

}
