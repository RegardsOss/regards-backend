package fr.cnes.regards.modules.storagelight.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.storagelight.domain.database.StorageLocationConfiguration;
import fr.cnes.regards.modules.storagelight.domain.plugin.StorageType;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IPrioritizedStorageRepository extends JpaRepository<StorageLocationConfiguration, Long> {

    /**
     * We want the {@link StorageLocationConfiguration} with the lowest priority, which means the highest value of the
     * attribute priority.
     * To do so, we order by descending priority and take the first one
     * @param storageType IStorageLocation type
     * @return the less prioritized
     */
    StorageLocationConfiguration findFirstByStorageTypeOrderByPriorityDesc(StorageType storageType);

    @Override
    Optional<StorageLocationConfiguration> findById(Long pluginConfId);

    Set<StorageLocationConfiguration> findAllByStorageTypeAndPriorityGreaterThanOrderByPriorityAsc(StorageType storageType,
            Long priority);

    List<StorageLocationConfiguration> findAllByStorageTypeOrderByPriorityAsc(StorageType storageType);

    /**
     * We want the active {@link StorageLocationConfiguration} with the highest priority, which means the lowest value of the
     * attribute priority.
     * To do so, we order by ascending priority and take the first one
     * @param storageType IStorageLocation type
     * @param pluginConfActivity the plugin configuration activeness
     * @return the most prioritized
     */
    StorageLocationConfiguration findFirstByStorageTypeAndStorageConfigurationActiveOrderByPriorityAsc(StorageType storageType,
            boolean pluginConfActivity);

    StorageLocationConfiguration findOneByStorageTypeAndPriority(StorageType storageType, long priority);

    Set<StorageLocationConfiguration> findAllByIdIn(Collection<Long> allIdUsedByAipInQuery);

    Set<StorageLocationConfiguration> findByStorageTypeAndStorageConfigurationLabelIn(StorageType storageType,
            Collection<String> confLabels);

    Set<StorageLocationConfiguration> findByStorageConfigurationLabelIn(Collection<String> confLabels);

    Optional<StorageLocationConfiguration> findByStorageConfigurationBusinessId(String businessId);
}
