package fr.cnes.regards.modules.processing.config.servlet;

import fr.cnes.regards.framework.feign.autoconfigure.FeignAutoConfiguration;
import fr.cnes.regards.framework.feign.autoconfigure.FeignSecurityAutoConfiguration;
import fr.cnes.regards.framework.feign.autoconfigure.FeignWebMvcConfiguration;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.processing.client.IReactiveRolesClient;
import fr.cnes.regards.modules.processing.client.IReactiveStorageClient;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Configuration
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "servlet", matchIfMissing = true)
@AutoConfigureAfter({
    FeignAutoConfiguration.class,
    FeignWebMvcConfiguration.class,
    FeignSecurityAutoConfiguration.class
})
@EnableFeignClients(basePackageClasses = {
        IRolesClient.class,
        IStorageRestClient.class
})
public class WrapFeignClientConfiguration {

    private static final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();

    @Bean
    public IReactiveRolesClient rolesReactiveClient(IRolesClient rolesClient) {
        return new IReactiveRolesClient() {
            @Override
            public Mono<Boolean> shouldAccessToResourceRequiring(String roleName, String authToken) {
                try {
                    ResponseEntity<Boolean> response = rolesClient.shouldAccessToResourceRequiring(roleName);
                    return Mono.just(response.getBody());
                } catch (EntityNotFoundException e) {
                    return Mono.error(e);
                }
            }
        };
    }

    @Bean
    public IReactiveStorageClient storageReactiveClient(IStorageRestClient storageClient) {
        return new IReactiveStorageClient() {
            @Override
            public Flux<DataBuffer> downloadFile(String checksum) {
                return DataBufferUtils.readInputStream(
                    () -> storageClient.downloadFile(checksum).body().asInputStream(),
                    bufferFactory,
                    4096
                );
            }
        };
    }

}
