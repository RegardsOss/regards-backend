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
import fr.cnes.regards.framework.security.event.UpdateAuthoritiesEvent;
import fr.cnes.regards.framework.security.utils.SpringBeanHelper;

/**
 *
 * Class UpdateSystemAuthoritiesListener
 *
 * This listener send an {@link UpdateAuthoritiesEvent} after each modification on the associated entity. The sent
 * {@link UpdateAuthoritiesEvent} allows to launch a refresh of microservices internal authorities configuration by
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
     * @param pEntity
     * @since 1.0-SNAPSHOT
     */
    @PostPersist
    @PostRemove
    @PostUpdate
    public void updateSystemAuthorities(final Object pEntity) {
        final IPublisher eventPublisher = SpringBeanHelper.getBean(IPublisher.class);
        if (eventPublisher != null) {
            eventPublisher.publish(new UpdateAuthoritiesEvent());
        } else {
            // FIXME : lancer une erreur! pourquoi ce if?
            LOG.error("Impossible to send update authorities event to cloud microservices. The authorities of microservices will not be updated");
        }
    }

}
