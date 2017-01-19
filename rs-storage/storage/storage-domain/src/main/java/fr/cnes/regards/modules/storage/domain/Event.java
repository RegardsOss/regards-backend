/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.hibernate.annotations.Type;

@Embeddable
public class Event {

    private String comment;

    private LocalDateTime date;

    private Event() {
    }

    public Event(String pComment) {
        comment = pComment;
        date = LocalDateTime.now();
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
    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime pDate) {
        date = pDate;
    }

}
