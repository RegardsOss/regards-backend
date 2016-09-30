/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp.domain;

/**
 * @author svissier
 *
 */
public class TestEvent {

    private String content_;

    public TestEvent() {

    }

    public TestEvent(final String pContent) {
        setContent(pContent);
    }

    public final String getContent() {
        return content_;
    }

    public final void setContent(String pContent) {
        content_ = pContent;
    }

    @Override
    public String toString() {
        return "{" + content_ + "}";
    }

    @Override
    public boolean equals(Object pO) {
        return (pO instanceof TestEvent) && ((TestEvent) pO).content_.equals(content_);
    }
}
