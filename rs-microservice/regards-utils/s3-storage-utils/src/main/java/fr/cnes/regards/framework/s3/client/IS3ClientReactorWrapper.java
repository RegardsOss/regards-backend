/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
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
