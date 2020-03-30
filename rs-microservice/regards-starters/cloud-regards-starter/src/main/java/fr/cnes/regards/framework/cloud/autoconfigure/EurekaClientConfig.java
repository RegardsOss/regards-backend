package fr.cnes.regards.framework.cloud.autoconfigure;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;

@Configuration
@EnableConfigurationProperties
@Profile("docker")
public class EurekaClientConfig {

    private final ConfigurableEnvironment env;

    public EurekaClientConfig(final ConfigurableEnvironment env) {
        this.env = env;
    }

    @Bean
    @Primary
    public EurekaInstanceConfigBean eurekaInstanceConfigBean(final InetUtils inetUtils) throws IOException {
        final String hostName = System.getenv("HOSTNAME");
        String hostAddress = null;

        final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netInt : Collections.list(networkInterfaces)) {
            for (InetAddress inetAddress : Collections.list(netInt.getInetAddresses())) {
                if (hostName.equals(inetAddress.getHostName())) {
                    hostAddress = inetAddress.getHostAddress();
                }

                //                System.out.printf("Inet %s: %s / %s\n", netInt.getName(),  inetAddress.getHostName(), inetAddress.getHostAddress());
            }
        }
        if (hostAddress == null) {
            throw new UnknownHostException("Cannot find ip address for hostname: " + hostName);
        }

        final int nonSecurePort = Integer.valueOf(env.getProperty("server.port", env.getProperty("port", "8080")));

        final EurekaInstanceConfigBean instance = new EurekaInstanceConfigBean(inetUtils);
        instance.setHostname(hostName);
        instance.setIpAddress(hostAddress);
        instance.setNonSecurePort(nonSecurePort);
        //        System.out.println(instance);
        return instance;
    }
}
