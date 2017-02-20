/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test.domain;

import fr.cnes.regards.framework.amqp.event.ISubscribable;

/**
 * @author svissier
 *
 */
public class TestEvent implements ISubscribable {

    /**
     * content sent
     */
    private String content;

    public TestEvent() {

    }

    public TestEvent(final String pContent) {
        setContent(pContent);
    }

    public final String getContent() {
        return content;
    }

    public final void setContent(String pContent) {
        content = pContent;
    }

    @Override
    public String toString() {
        return "{\"content\" : " + content + "}";
    }

    @Override
    public boolean equals(Object pO) {
        return (pO instanceof TestEvent) && ((TestEvent) pO).content.equals(content);
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }
}
