/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 *
 * helper class for test purpose, represent a rabbitmq queue retrieved from the rest api
 *
 * @author svissier
 *
 */
@JsonIgnoreProperties
public class RestQueue {

    /**
     * name of the queue
     */
    private String name;

    /**
     * virtual host on which queue is defined
     */
    private String vhost;

    /**
     * either the queue is durable
     */
    private boolean durable;

    /**
     * either the queue is exclusive
     */
    private boolean exclusive;

    /**
     * either the queue is auto deleting
     */
    private boolean autoDelete;

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

    public boolean isDurable() {
        return durable;
    }

    public void setDurable(boolean pDurable) {
        durable = pDurable;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public void setExclusive(boolean pExclusive) {
        exclusive = pExclusive;
    }

    // CHECKSTYLE:OFF
    public boolean isAuto_delete() {
        return autoDelete;
    }

    public void setAuto_delete(boolean pAuto_delete) {
        autoDelete = pAuto_delete;
    }
    // CHECKTYLE:ON
}
