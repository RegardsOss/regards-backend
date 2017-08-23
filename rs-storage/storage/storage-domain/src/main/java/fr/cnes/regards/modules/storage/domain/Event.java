/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.time.OffsetDateTime;

import org.hibernate.annotations.Type;

@Embeddable
public class Event {

    @Column
    @Type(type = "text")
    private String comment;

    @Column
    private OffsetDateTime date;

    @Column
    @Enumerated(EnumType.STRING)
    private EventType type;

    private Event() {
    }

    public Event(String pComment) {
        comment = pComment;
        date = OffsetDateTime.now();
    }

    public Event(String comment, OffsetDateTime date) {
        this(comment,date,null);
    }

    public Event(String comment, OffsetDateTime date, EventType type) {
        this.comment = comment;
        this.date = date;
        this.type=type;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String pComment) {
        comment = pComment;
    }

    public OffsetDateTime getDate() {
        return date;
    }

    public void setDate(OffsetDateTime pDate) {
        date = pDate;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }
}
