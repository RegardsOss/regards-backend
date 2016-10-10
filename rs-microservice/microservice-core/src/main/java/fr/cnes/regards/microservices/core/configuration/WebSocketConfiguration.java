/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.configuration;

import java.util.List;

import org.eclipse.jetty.websocket.api.WebSocketBehavior;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

    private static final int BUFFER_SIZE = 8192;

    private static final long TIMEOUT = 600000;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint to which the client must access to connect to websocket server
        registry.addEndpoint("/wsconnect").setHandshakeHandler(handshakeHandler()).setAllowedOrigins("*").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // endpoint to which websocket client should listen to get messages
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/myApp");

    }

    @Bean
    public DefaultHandshakeHandler handshakeHandler() {

        WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.SERVER);
        policy.setInputBufferSize(BUFFER_SIZE);
        policy.setIdleTimeout(TIMEOUT);

        return new DefaultHandshakeHandler(new JettyRequestUpgradeStrategy(new WebSocketServerFactory(policy)));
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> arg0) {
        // Not implemented
    }

    @Override
    public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> arg0) {
        // Not implemented
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration arg0) {
        // Not implemented
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration arg0) {
        // Not implemented
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> arg0) {
        return false;
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration arg0) {
        // Not implemented
    }

}
