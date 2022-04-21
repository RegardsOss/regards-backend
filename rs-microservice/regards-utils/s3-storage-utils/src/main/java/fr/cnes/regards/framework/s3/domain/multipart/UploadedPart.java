package fr.cnes.regards.framework.s3.domain.multipart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import software.amazon.awssdk.services.s3.model.CompletedPart;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class UploadedPart {

    CompletedPart completed;

    long size;
}
