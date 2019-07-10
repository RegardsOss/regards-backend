package fr.cnes.regards.modules.storagelight.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.storagelight.domain.database.PrioritizedDataStorage;
import fr.cnes.regards.modules.storagelight.domain.plugin.DataStorageType;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IPrioritizedDataStorageRepository extends JpaRepository<PrioritizedDataStorage, Long> {

    /**
     * We want the {@link PrioritizedDataStorage} with the lowest priority, which means the highest value of the
     * attribute priority.
     * To do so, we order by descending priority and take the first one
     * @param dataStorageType IDataStorage type
     * @return the less prioritized
     */
    PrioritizedDataStorage findFirstByDataStorageTypeOrderByPriorityDesc(DataStorageType dataStorageType);

    @Override
    @EntityGraph(attributePaths = { "dataStorageConfiguration.parameters",
            "dataStorageConfiguration.parameters.dynamicsValues" })
    Optional<PrioritizedDataStorage> findById(Long pluginConfId);

    Set<PrioritizedDataStorage> findAllByDataStorageTypeAndPriorityGreaterThanOrderByPriorityAsc(
            DataStorageType dataStorageType, Long priority);

    @EntityGraph(attributePaths = { "dataStorageConfiguration.parameters",
            "dataStorageConfiguration.parameters.dynamicsValues" })
    List<PrioritizedDataStorage> findAllByDataStorageTypeOrderByPriorityAsc(DataStorageType dataStorageType);

    /**
     * We want the active {@link PrioritizedDataStorage} with the highest priority, which means the lowest value of the
     * attribute priority.
     * To do so, we order by ascending priority and take the first one
     * @param dataStorageType IDataStorage type
     * @param pluginConfActivity the plugin configuration activeness
     * @return the most prioritized
     */
    PrioritizedDataStorage findFirstByDataStorageTypeAndDataStorageConfigurationActiveOrderByPriorityAsc(
            DataStorageType dataStorageType, boolean pluginConfActivity);

    PrioritizedDataStorage findOneByDataStorageTypeAndPriority(DataStorageType dataStorageType, long priority);

    @EntityGraph(attributePaths = { "dataStorageConfiguration.parameters",
            "dataStorageConfiguration.parameters.dynamicsValues" })
    Set<PrioritizedDataStorage> findAllByIdIn(Collection<Long> allIdUsedByAipInQuery);
}
