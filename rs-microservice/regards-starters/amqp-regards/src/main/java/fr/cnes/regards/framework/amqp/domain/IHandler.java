/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.support.InterceptingHttpAccessor;

/**
 * @param <T>
 *            Type of Event you are handling
 *
 *            Interface identifying classes that can handle message from the broker
 *
 * @author svissier
 *
 */
public interface IHandler<T> {

    static final Logger LOGGER= LoggerFactory.getLogger(IHandler.class);

    public default void handleAndLog(TenantWrapper<T> pWrapper) {
        LOGGER.info("Received {}, From {}", pWrapper.getContent().getClass().getSimpleName(), pWrapper.getTenant());
        handle(pWrapper);
    }

    public void handle(TenantWrapper<T> pWrapper);

    @SuppressWarnings("unchecked")
    public default Class<? extends IHandler<T>> getType() {
        return (Class<? extends IHandler<T>>) this.getClass();
    }
}
