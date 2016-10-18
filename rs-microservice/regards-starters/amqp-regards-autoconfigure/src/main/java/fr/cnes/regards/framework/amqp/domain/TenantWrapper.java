/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

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

    public TenantWrapper(T pContent, String pTenant) {
        content = pContent;
        tenant = pTenant;
    }

    @JsonTypeInfo(use = Id.CLASS)
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
