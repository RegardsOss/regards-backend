package fr.cnes.regards.modules.feature.dao;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Repository
public interface IAbstractFeatureRequest extends JpaRepository<AbstractFeatureRequest, Long> {

    @Query("select distinct afr.requestId from AbstractFeatureRequest afr")
    Set<String> findRequestId();

}
