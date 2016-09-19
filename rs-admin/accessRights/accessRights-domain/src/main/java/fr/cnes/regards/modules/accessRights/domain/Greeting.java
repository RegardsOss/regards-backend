/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.domain;

import org.springframework.hateoas.Identifiable;

public class Greeting implements Identifiable<Long> {

    private final Long id_;

    private final String content_;

    public Greeting(long id, String content) {
        this.id_ = id;
        this.content_ = content;
    }

    @Override
    public Long getId() {
        return id_;
    }

    public String getContent() {
        return content_;
    }
}
