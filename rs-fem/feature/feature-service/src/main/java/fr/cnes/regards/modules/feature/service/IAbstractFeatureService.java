package fr.cnes.regards.modules.feature.service;

import fr.cnes.regards.framework.amqp.event.IRequestDeniedService;
import fr.cnes.regards.framework.amqp.event.IRequestValidation;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestType;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IAbstractFeatureService extends IRequestDeniedService, IRequestValidation {

}
