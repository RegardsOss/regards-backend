/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class Event {

    @Column
    private String comment;

    @Column
    private LocalDateTime eventDate;

    public Event(String comment) {
        this.comment = comment;
        eventDate = LocalDateTime.now();
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String pComment) {
        comment = pComment;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime pEventDate) {
        eventDate = pEventDate;
    }

}
