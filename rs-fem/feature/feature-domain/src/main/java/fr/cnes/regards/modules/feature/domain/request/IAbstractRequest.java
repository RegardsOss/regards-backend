package fr.cnes.regards.modules.feature.domain.request;

import java.time.OffsetDateTime;

import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;

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
