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
@FunctionalInterface
public interface IHandler<T> {

    public void handle(TenantWrapper<T> pT);
}
