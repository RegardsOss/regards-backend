package fr.cnes.regards.framework.s3.domain;

import lombok.Value;

import java.util.UUID;

@Value
public class StorageCommandID {

    String taskId;

    UUID commandId;
}
