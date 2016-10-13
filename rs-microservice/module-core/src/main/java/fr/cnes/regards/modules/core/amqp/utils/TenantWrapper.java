/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @param <T>
 *            type of event that is wrapped
 *
 * @author svissier
 *
 */
public class TenantWrapper<T> {

    /**
     * the event originally sent to the message broker
     */
    private T content;

    /**
     * Tenant from which the message comes from
     */
    private String tenant;

    public TenantWrapper() {
    }

    @JsonCreator
    public TenantWrapper(@JsonProperty("content") T pContent, @JsonProperty("tenant") String pTenant) {
        content = pContent;
        tenant = pTenant;
    }

    public T getContent() {
        return content;
    }

    public void setContent(T pContent) {
        content = pContent;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String pTenant) {
        tenant = pTenant;
    }

    @Override
    public String toString() {
        return "{\"content\" : " + content + " , \"tenant\" : \"" + tenant + "\"}";
    }
}
