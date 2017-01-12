package fr.cnes.regards.modules.storage.domain;

import java.time.LocalDateTime;

public class Event {

    private String comment;

    private LocalDateTime eventDate;

    public Event(String comment) {
        this.comment = comment;
        this.eventDate = LocalDateTime.now();
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
