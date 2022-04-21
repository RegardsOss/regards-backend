package fr.cnes.regards.framework.s3.domain.multipart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import reactor.core.publisher.Flux;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.nio.ByteBuffer;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class ResponseAndStream {

    GetObjectResponse response;

    Flux<ByteBuffer> stream;
}

