/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper.TypePrecedence;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.core.amqp.provider.IProjectsProvider;
import fr.cnes.regards.modules.core.amqp.utils.Handler;
import fr.cnes.regards.modules.core.amqp.utils.TenantWrapper;
import fr.cnes.regards.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.security.utils.jwt.JWTService;
import fr.cnes.regards.security.utils.jwt.exception.InvalidJwtException;
import fr.cnes.regards.security.utils.jwt.exception.MissingClaimException;

/**
 * @author svissier
 *
 */
@Component
public class Subscriber {

    @Autowired
    private RabbitAdmin rabbitAdmin_;

    @Autowired
    private Jackson2JsonMessageConverter jackson2JsonMessageConverter_;

    @Autowired
    private IProjectsProvider projectsProvider_;

    @Autowired
    private JWTService jwtService_;

    /**
     *
     *
     * @param pEvt
     *            the event class token you want to subscribe to
     * @param pReceiver
     *            the POJO defining the method handling the corresponding event
     * @param pConnectionFactory
     *            connection factory from context
     * @return the container initialized with right values
     */
    public final SimpleMessageListenerContainer subscribeTo(Class<?> pEvt, Handler pReceiver,
            ConnectionFactory pConnectionFactory) {
        List<String> projects = projectsProvider_.retrieveProjectList();
        List<DirectExchange> exchanges = projects.stream().map(p -> new DirectExchange(p)).collect(Collectors.toList());
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        jackson2JsonMessageConverter_.setTypePrecedence(TypePrecedence.TYPE_ID);
        for (DirectExchange exchange : exchanges) {
            rabbitAdmin_.declareExchange(exchange);
            Queue queue = new Queue(pEvt.getName(), true);
            rabbitAdmin_.declareQueue(queue);
            Binding binding = BindingBuilder.bind(queue).to(exchange).with(pEvt.getName());
            rabbitAdmin_.declareBinding(binding);
            container.setConnectionFactory(pConnectionFactory);
            container.setRabbitAdmin(rabbitAdmin_);
            MessageListenerAdapter messageListener = new MessageListenerAdapter(new TenantWrapperReceiver(pReceiver),
                    "dewrap");
            messageListener.setMessageConverter(jackson2JsonMessageConverter_);
            container.setMessageListener(messageListener);
            container.addQueues(queue);
        }
        return container;
    }

    private class TenantWrapperReceiver {

        private final Handler handler_;

        /**
         *
         */
        public TenantWrapperReceiver(Handler pHandler) {
            handler_ = pHandler;
        }

        /**
         *
         * @param pWrappedMessage
         * @throws InvalidJwtException
         * @throws MissingClaimException
         */
        public final void dewrap(TenantWrapper pWrappedMessage) throws InvalidJwtException, MissingClaimException {
            String jwt = jwtService_.generateToken(pWrappedMessage.getTenant(), "", "", "ADMIN");
            SecurityContextHolder.getContext().setAuthentication(jwtService_.parseToken(new JWTAuthentication(jwt)));
            handler_.handle(pWrappedMessage.getContent());
        }

    }

}
