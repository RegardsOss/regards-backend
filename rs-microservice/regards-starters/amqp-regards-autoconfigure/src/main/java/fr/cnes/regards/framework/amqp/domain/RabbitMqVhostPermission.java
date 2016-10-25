/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.domain;

/**
 * @author svissier
 *
 */
public class RabbitMqVhostPermission {

    /**
     * syntax allowing to give all rights
     */
    private static final String ALL_RIGHTS = ".*";

    /**
     * fields required by RabbitMQ Http API, represents rights to configure the virtual host
     */
    private final String configure;

    /**
     * fields required by RabbitMQ Http API, represents rights to write into the virtual host
     */
    private final String write;

    /**
     * fields required by RabbitMQ Http API, represents rights to read from the virtual host
     */
    private final String read;

    public RabbitMqVhostPermission() {
        configure = ALL_RIGHTS;
        write = ALL_RIGHTS;
        read = ALL_RIGHTS;
    }

    public String getConfigure() {
        return configure;
    }

    public String getWrite() {
        return write;
    }

    public String getRead() {
        return read;
    }

}
