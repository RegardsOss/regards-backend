package fr.cnes.regards.modules.feature.domain.request;

import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;

import java.time.OffsetDateTime;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IAbstractRequest {

    String getRequestId();

    OffsetDateTime getRequestDate();

    OffsetDateTime getRegistrationDate();

    FeatureRequestStep getStep();

    PriorityLevel getPriority();

    RequestState getState();

    String getRequestOwner();
}
