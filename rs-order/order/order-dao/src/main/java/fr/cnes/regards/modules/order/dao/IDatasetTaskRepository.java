package fr.cnes.regards.modules.order.dao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.order.domain.DatasetTask;

/**
 * @author oroussel
 */
public interface IDatasetTaskRepository extends JpaRepository<DatasetTask, Long> {
    @EntityGraph("graph.complete")
    DatasetTask findCompleteById(Long id);
}
