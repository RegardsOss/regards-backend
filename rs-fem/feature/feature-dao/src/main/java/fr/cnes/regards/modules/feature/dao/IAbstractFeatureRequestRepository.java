package fr.cnes.regards.modules.feature.dao;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Repository
public interface IAbstractFeatureRequestRepository<T extends AbstractFeatureRequest> extends JpaRepository<T, Long> {

    @Query("select distinct afr.requestId from AbstractFeatureRequest afr")
    Set<String> findRequestId();

    Set<T> findAllByRequestIdIn(List<String> requestIds);

    /**
     * Get a page of {@link T} with specified step.
     * @param requestDate current date we will not schedule future requests
     * @return a list of {@link T}
     */
    Page<T> findByStepAndRequestDateLessThanEqual(FeatureRequestStep step, OffsetDateTime requestDate, Pageable page);

    /**
     * Update {@link AbstractFeatureRequest} step. <b>WARNING: this method acts on {@link AbstractFeatureRequest}
     * so, for example, even using a {@link IFeatureCopyRequestRepository} you can update a {@link fr.cnes.regards.modules.feature.domain.request.FeatureUpdateRequest}</b>
     * @param step new {@link FeatureRequestStep}
     * @param ids id of {@link AbstractFeatureRequest} to update
     */
    @Modifying
    @Query("update AbstractFeatureRequest afr set afr.step = :newStep where afr.id in :ids ")
    void updateStep(@Param("newStep") FeatureRequestStep step, @Param("ids") Set<Long> ids);

    /**
     * Update {@link AbstractFeatureRequest} state. <b>WARNING: this method acts on {@link AbstractFeatureRequest}
     * so, for example, even using a {@link IFeatureCopyRequestRepository} you can update a {@link fr.cnes.regards.modules.feature.domain.request.FeatureUpdateRequest}</b>
     * @param requestState new {@link FeatureRequestStep}
     * @param ids id of {@link AbstractFeatureRequest} to update
     */
    @Modifying
    @Query("update AbstractFeatureRequest afr set afr.state = :newState where afr.id in :ids ")
    void updateState(@Param("newState") RequestState requestState, @Param("ids") Set<Long> ids);

    void deleteByUrnIn(Set<FeatureUniformResourceName> collect);
}
