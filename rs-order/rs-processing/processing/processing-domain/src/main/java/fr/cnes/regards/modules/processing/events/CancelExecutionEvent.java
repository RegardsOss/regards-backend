package fr.cnes.regards.modules.processing.events;

import lombok.Value;

import java.util.UUID;

@Value
public class CancelExecutionEvent {

    UUID executionId;
    String message;

}
