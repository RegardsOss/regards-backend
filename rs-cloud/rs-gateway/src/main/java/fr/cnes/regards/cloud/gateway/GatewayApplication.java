package fr.cnes.regards.cloud.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

import fr.cnes.regards.client.core.ClientRequestInterceptor;

@SpringBootApplication(scanBasePackages = "fr.cnes.regards")
@EnableFeignClients(basePackages = "fr.cnes.regards", defaultConfiguration = { ClientRequestInterceptor.class })
@EnableZuulProxy
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
