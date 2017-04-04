/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.domain;

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

    public void handle(TenantWrapper<T> pWrapper);

    @SuppressWarnings("unchecked")
    public default Class<? extends IHandler<T>> getType() {
        return (Class<? extends IHandler<T>>) this.getClass();
    }
}
