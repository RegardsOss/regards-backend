/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author svissier
 *
 */
@JsonIgnoreProperties
public class RestBinding {

    private String vhost;

    private String destination;

    private String source;

    private String routing_key;

    public String getVhost() {
        return vhost;
    }

    public void setVhost(String pVhost) {
        vhost = pVhost;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String pDestination) {
        destination = pDestination;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String pSource) {
        source = pSource;
    }

    public String getRouting_key() {
        return routing_key;
    }

    public void setRouting_key(String pRouting_key) {
        routing_key = pRouting_key;
    }

}
