package fr.cnes.regards.modules.feature.service;

import fr.cnes.regards.framework.amqp.event.IRequestDeniedService;
import fr.cnes.regards.framework.amqp.event.IRequestValidation;
import fr.cnes.regards.modules.feature.dto.FeatureRequestSearchParameters;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsInfo;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IAbstractFeatureService extends IRequestDeniedService, IRequestValidation {

    /**
     * Find requests information with search parameters context
     * @param searchParameters {@link FeatureRequestSearchParameters}
     * @return {@link RequestsInfo}
     */
    RequestsInfo getInfo(FeatureRequestSearchParameters searchParameters);

}
