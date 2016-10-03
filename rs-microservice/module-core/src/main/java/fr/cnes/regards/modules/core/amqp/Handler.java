/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp;

/**
 * @author svissier
 *
 */
public interface Handler<T> {

    public void handle(T t);
}
