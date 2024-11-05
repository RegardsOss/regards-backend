package fr.cnes.regards.modules.feature.dao;

import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.IProviderIdByUrn;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Sylvain VISSIERE-GUERINET
 * @author Sébastien Binda
 */
@Repository
public interface IAbstractFeatureRequestRepository<T extends AbstractFeatureRequest>
    extends JpaRepository<T, Long>, JpaSpecificationExecutor<T> {

    Page<T> findByStepAndGroupIdIn(FeatureRequestStep step, Collection<String> groupIds, Pageable page);

    @Query("select distinct afr.requestId from AbstractFeatureRequest afr")
    Set<String> findRequestId();

    @Query("select requestId from AbstractFeatureRequest where requestId in (:requestIds)")
    Set<String> findRequestIdByRequestIdIn(@Param("requestIds") List<String> requestIds);

    @Query(
        "select f.urn as urn, f.providerId as providerId from FeatureEntity f, AbstractFeatureRequest r where r.urn in :urns and r.urn=f.urn")
    List<IProviderIdByUrn> findFeatureProviderIdFromRequestUrns(@Param("urns") List<FeatureUniformResourceName> urns);

    Set<T> findAllByRequestIdIn(List<String> requestIds);

    /**
     * Get a page of {@link T} with specified step.
     *
     * @param requestDate current date we will not schedule future requests
     * @return a list of {@link T}
     */
    Page<T> findByStepAndRequestDateLessThanEqual(FeatureRequestStep step, OffsetDateTime requestDate, Pageable page);

    /**
     * Update {@link AbstractFeatureRequest} step. <b>WARNING: this method acts on {@link AbstractFeatureRequest}
     * so, for example, even using a {@link IFeatureCopyRequestRepository} you can update a {@link fr.cnes.regards.modules.feature.domain.request.FeatureUpdateRequest}</b>
     *
     * @param step new {@link FeatureRequestStep}
     * @param ids  id of {@link AbstractFeatureRequest} to update
     */
    @Modifying
    @Query("UPDATE AbstractFeatureRequest afr SET afr.step = :newStep WHERE afr.id IN :ids ")
    void updateStep(@Param("newStep") FeatureRequestStep step, @Param("ids") Set<Long> ids);

    @Modifying
    @Query("delete from AbstractFeatureRequest req where urn in :urns")
    void deleteByUrnIn(@Param("urns") Set<FeatureUniformResourceName> urns);

    Page<T> findByStep(FeatureRequestStep remoteStorageRequested, Pageable pageToRequest);

    List<T> findAllByUrnInAndStep(List<FeatureUniformResourceName> urn, FeatureRequestStep step);

    @Modifying
    @Query(value = "UPDATE t_feature_request SET registration_date = :date WHERE id IN :ids", nativeQuery = true)
    void forceRegistrationDate(@Param("ids") List<Long> list, @Param("date") OffsetDateTime offsetDateTime);
}
