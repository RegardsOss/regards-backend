package fr.cnes.regards.modules.processing.config.reactive;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "reactive")
public class ProcessingNettyReactiveConfiguration {

    @Value("${server.port}")
    private int port;

    @Bean
    public NettyReactiveWebServerFactory nettyReactiveWebServerFactory() {
        return new NettyReactiveWebServerFactory(port);
    }

}
