package fr.cnes.regards.framework.s3.domain.multipart;

import io.vavr.collection.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import software.amazon.awssdk.services.s3.model.CompletedPart;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class MultipartReport {

    List<CompletedPart> completed;

    long accumulatedSize;

    public MultipartReport() {
        this(List.empty(), 0L);
    }

    public MultipartReport accumulate(UploadedPart up) {
        return new MultipartReport(completed.append(up.getCompleted()), accumulatedSize + up.getSize());
    }
}
