package fr.cnes.regards.modules.feature.service;

import fr.cnes.regards.framework.amqp.event.IRequestDeniedService;
import fr.cnes.regards.framework.amqp.event.IRequestValidation;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IAbstractFeatureService extends IRequestDeniedService, IRequestValidation {

}
