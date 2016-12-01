/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.entity.listeners;

import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.security.event.UpdateAuthoritiesEvent;
import fr.cnes.regards.framework.security.utils.SpringBeanHelper;

/**
 *
 * Class UpdateSystemAuthoritiesListener
 *
 * This listener send an {@link UpdateAuthoritiesEvent} after each modification on the associated entity. The vent
 * {@link UpdateAuthoritiesEvent}, allow to launch a refresh of microservices internal authorities configuration by
 * communication with administration service.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Configurable
public class UpdateAuthoritiesListener {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(UpdateAuthoritiesListener.class);

    /**
     *
     * Publish an AMQ Event {@link UpdateAuthoritiesEvent} to indicates to all microservice that the authorities
     * configuration has changed.
     *
     * @since 1.0-SNAPSHOT
     */
    @PostPersist
    @PostRemove
    @PostUpdate
    public void updateSystemAuhtorities(final Object pEntity) {
        try {
            final IPublisher eventPublisher = SpringBeanHelper.getBean(IPublisher.class);
            eventPublisher.publish(UpdateAuthoritiesEvent.class, AmqpCommunicationMode.ONE_TO_MANY,
                                   AmqpCommunicationTarget.EXTERNAL);
        } catch (final RabbitMQVhostException e) {
            LOG.error(e.getMessage(), e);
        }
    }

}
