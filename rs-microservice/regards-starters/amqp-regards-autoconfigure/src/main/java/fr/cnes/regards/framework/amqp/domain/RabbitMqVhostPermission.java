/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.domain;

/**
 * @author svissier
 *
 */
public class RabbitMqVhostPermission {

    private static final String DEFAULT_CONFIGURE_RIGHTS = ".*";

    private static final String DEFAULT_WRITE_RIGHTS = ".*";

    private static final String DEFAULT_READ_RIGHTS = ".*";

    private String configure;

    private String write;

    private String read;

    public RabbitMqVhostPermission() {
        configure = DEFAULT_CONFIGURE_RIGHTS;
        write = DEFAULT_WRITE_RIGHTS;
        read = DEFAULT_READ_RIGHTS;
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
