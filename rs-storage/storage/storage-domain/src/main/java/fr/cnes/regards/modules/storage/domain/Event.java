/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.hibernate.annotations.Type;

@Embeddable
public class Event {

    private String comment;

    private OffsetDateTime eventDate;

    private Event() {
    }

    public Event(String pComment) {
        comment = pComment;
        eventDate = OffsetDateTime.now();
    }

    public Event(String pComment, OffsetDateTime pDate) {
        comment = pComment;
        eventDate = pDate;
    }

    @Column
    @Type(type = "text")
    public String getComment() {
        return comment;
    }

    public void setComment(String pComment) {
        comment = pComment;
    }

    @Column
    public OffsetDateTime getDate() {
        return eventDate;
    }

    public void setDate(OffsetDateTime pDate) {
        eventDate = pDate;
    }

}
