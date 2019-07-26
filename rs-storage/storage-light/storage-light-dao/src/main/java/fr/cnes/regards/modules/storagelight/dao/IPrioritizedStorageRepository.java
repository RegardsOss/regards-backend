package fr.cnes.regards.modules.storagelight.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.storagelight.domain.database.PrioritizedStorage;
import fr.cnes.regards.modules.storagelight.domain.plugin.StorageType;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IPrioritizedStorageRepository extends JpaRepository<PrioritizedStorage, Long> {

    /**
     * We want the {@link PrioritizedStorage} with the lowest priority, which means the highest value of the
     * attribute priority.
     * To do so, we order by descending priority and take the first one
     * @param storageType IStorageLocation type
     * @return the less prioritized
     */
    PrioritizedStorage findFirstByStorageTypeOrderByPriorityDesc(StorageType storageType);

    @Override
    @EntityGraph(
            attributePaths = { "storageConfiguration.parameters", "storageConfiguration.parameters.dynamicsValues" })
    Optional<PrioritizedStorage> findById(Long pluginConfId);

    Set<PrioritizedStorage> findAllByStorageTypeAndPriorityGreaterThanOrderByPriorityAsc(StorageType storageType,
            Long priority);

    @EntityGraph(
            attributePaths = { "storageConfiguration.parameters", "storageConfiguration.parameters.dynamicsValues" })
    List<PrioritizedStorage> findAllByStorageTypeOrderByPriorityAsc(StorageType storageType);

    /**
     * We want the active {@link PrioritizedStorage} with the highest priority, which means the lowest value of the
     * attribute priority.
     * To do so, we order by ascending priority and take the first one
     * @param storageType IStorageLocation type
     * @param pluginConfActivity the plugin configuration activeness
     * @return the most prioritized
     */
    PrioritizedStorage findFirstByStorageTypeAndStorageConfigurationActiveOrderByPriorityAsc(StorageType storageType,
            boolean pluginConfActivity);

    PrioritizedStorage findOneByStorageTypeAndPriority(StorageType storageType, long priority);

    @EntityGraph(
            attributePaths = { "storageConfiguration.parameters", "storageConfiguration.parameters.dynamicsValues" })
    Set<PrioritizedStorage> findAllByIdIn(Collection<Long> allIdUsedByAipInQuery);

    Set<PrioritizedStorage> findByStorageTypeAndStorageConfigurationLabelIn(StorageType storageType,
            Collection<String> confLabels);
}
