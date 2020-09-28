package fr.cnes.regards.modules.processing.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactor.core.publisher.Flux;

@ReactiveFeignClient(name = "rs-storage")
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "reactive")

public interface IReactiveStorageClient {

    String FILE_PATH = "/files";

    String DOWNLOAD_PATH = "/{checksum}/download";

    @GetMapping(FILE_PATH + DOWNLOAD_PATH)
    Flux<DataBuffer> downloadFile(@PathVariable("checksum") String checksum);

}
