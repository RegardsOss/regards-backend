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
    private String configure;

    /**
     * fields required by RabbitMQ Http API, represents rights to write into the virtual host
     */
    private String write;

    /**
     * fields required by RabbitMQ Http API, represents rights to read from the virtual host
     */
    private String read;

    public RabbitMqVhostPermission() {
        configure = ALL_RIGHTS;
        write = ALL_RIGHTS;
        read = ALL_RIGHTS;
    }

    public String getConfigure() {
        return configure;
    }

    public void setConfigure(String pConfigure) {
        configure = pConfigure;
    }

    public String getWrite() {
        return write;
    }

    public void setWrite(String pWrite) {
        write = pWrite;
    }

    public String getRead() {
        return read;
    }

    public void setRead(String pRead) {
        read = pRead;
    }

}
