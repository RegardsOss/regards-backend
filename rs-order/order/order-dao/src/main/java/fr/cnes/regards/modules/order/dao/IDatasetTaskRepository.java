package fr.cnes.regards.modules.order.dao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.order.domain.DatasetTask;

/**
 * @author oroussel
 */
@Repository
public interface IDatasetTaskRepository extends JpaRepository<DatasetTask, Long> {
    @EntityGraph("graph.datasetTask.complete")
    DatasetTask findCompleteById(Long id);
}
