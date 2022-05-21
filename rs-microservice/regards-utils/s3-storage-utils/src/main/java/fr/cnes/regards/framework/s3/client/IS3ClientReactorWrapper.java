package fr.cnes.regards.framework.s3.client;

import fr.cnes.regards.framework.s3.domain.multipart.ResponseAndStream;
import fr.cnes.regards.framework.s3.domain.multipart.UploadedPart;
import io.vavr.collection.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.InputStream;

public interface IS3ClientReactorWrapper {

    Mono<Boolean> exists(String bucket, String key);

    Mono<ResponseAndStream> readContentFlux(String bucket, String key, boolean failIfMissing);

    Flux<String> listObjects(String bucket, String prefix);

    Mono<PutObjectResponse> putContent(String bucket, String path, InputStream content);

    Mono<PutObjectResponse> putContent(String bucket, String path, byte[] content);

    Mono<String> initiateMultipartUpload(String bucket, String path);

    Mono<UploadedPart> uploadMultipartFilePart(String bucket,
                                               String path,
                                               String uploadId,
                                               int partId,
                                               byte[] partBytes);

    Mono<String> completeMultipartUpload(String bucket, String path, String uploadId, List<CompletedPart> parts);

    Mono<String> abortMultipartUpload(String bucket, String path, String uploadId);

    Mono<DeleteObjectResponse> deleteContent(String bucket, String path);

    Flux<DeleteObjectResponse> deleteContentWithPrefix(String bucket, String prefix);

}
