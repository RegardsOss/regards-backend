package fr.cnes.regards.modules.processing.client;

import feign.Param;
import feign.RequestLine;
import org.springframework.core.io.buffer.DataBuffer;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactor.core.publisher.Flux;

@ReactiveFeignClient(name = "rs-storage")
public interface IReactiveStorageClient {

    String FILE_PATH = "/files";

    String DOWNLOAD_PATH = "/{checksum}/download";

    @RequestLine("GET " + FILE_PATH + DOWNLOAD_PATH)
    Flux<DataBuffer> downloadFile(@Param("checksum") String checksum);

}
