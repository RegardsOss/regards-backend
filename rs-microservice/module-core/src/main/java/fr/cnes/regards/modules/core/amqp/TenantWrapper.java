/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp;

/**
 * @author svissier
 *
 */
public class TenantWrapper<T> {

    private T content_;

    private String tenant_;

    public TenantWrapper(T content, String tenant) {
        content_ = content;
        tenant_ = tenant;
    }

    public T getContent() {
        return content_;
    }

    public void setContent(T pContent) {
        content_ = pContent;
    }

    public String getTenant() {
        return tenant_;
    }

    public void setTenant(String pTenant) {
        tenant_ = pTenant;
    }
}
