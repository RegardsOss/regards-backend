/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test.domain;

/**
 * @author svissier
 *
 */
public class RestExchange {

    private String name;

    private String vhost;

    private String type;

    private boolean durable;

    private boolean auto_delete;

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }

    public String getVhost() {
        return vhost;
    }

    public void setVhost(String pVhost) {
        vhost = pVhost;
    }

    public String getType() {
        return type;
    }

    public void setType(String pType) {
        type = pType;
    }

    public boolean isDurable() {
        return durable;
    }

    public void setDurable(boolean pDurable) {
        durable = pDurable;
    }

    public boolean isAuto_delete() {
        return auto_delete;
    }

    public void setAuto_delete(boolean pAuto_delete) {
        auto_delete = pAuto_delete;
    }

}
