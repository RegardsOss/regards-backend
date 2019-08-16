package fr.cnes.regards.modules.acquisition.service.session;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionMonitoringEvent;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationOperator;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service that sends notification to collect stats about ingestion
 * This service works by session owner, in this µservice it corresponds to the chain label,
 * and a session name, provided by the user when he starts manually the chain or when the chain starts automatically the current date
 * @author Léo Mieulet
 */
@Service
public class SessionNotifier {
    /**
     * The name of the property gathering all metadata about this processing step
     */
    private static final String GLOBAL_SESSION_STEP = "PRODUCTS";

    /**
     * Name of the property that stores the current state (running, done)
     */
    private static final String PROPERTY_STATE = "state";

    /**
     * PROPERTY_STATE value
     */
    private static final String STATE_VALUE_START = "RUNNING";

    /**
     * PROPERTY_STATE value
     */
    private static final String STATE_VALUE_STOP = "DONE";

    /**
     * Name of the property that collects number of products generated
     */
    private static final String PROPERTY_GENERATED = "generated";

    /**
     * Name of the property that collects number of products incomplete
     */
    private static final String PROPERTY_INCOMPLETE = "incomplete";

    /**
     * Name of the property that collects number of products invalid (too many files attached to a single product)
     */
    private static final String PROPERTY_INVALID = "invalid";

    /**
     * Name of the property that collects number of products that can't product SIP
     */
    private static final String PROPERTY_ERROR = "error";


    @Autowired
    private IPublisher publisher;

    public void notifyProductStateChanges(Product currentProduct, ProductState oldState) {
        if (currentProduct.getState() != oldState && !(
                // Ignore FINISHED -> COMPLETED state change
                currentProduct.getState() == ProductState.FINISHED && oldState == ProductState.COMPLETED
        )) {
            // First decrement the old state
            if (oldState != null) {
                notifyDecrementSession(currentProduct.getProcessingChain().getLabel(),
                        currentProduct.getSession(), oldState);
            }
            // increment the new state
            switch (currentProduct.getState()) {
                case ACQUIRING:
                case COMPLETED:
                case FINISHED:
                    notifyIncrementSession(currentProduct);
                    break;
                case INVALID:
                    notifyIncrementSessionError(currentProduct);
                    break;
                case UPDATED:
                    // Don't need to track such products
            }
        }
    }


    public void notifyStartingChain(String sessionOwner, String session) {
        publisher.publish(SessionMonitoringEvent.build(
                sessionOwner,
                session,
                SessionNotificationState.OK,
                GLOBAL_SESSION_STEP,
                SessionNotificationOperator.REPLACE,
                PROPERTY_STATE,
                STATE_VALUE_START
        ));
    }

    public void notifyEndingChain(String sessionOwner, String session) {
        publisher.publish(SessionMonitoringEvent.build(
                sessionOwner,
                session,
                SessionNotificationState.OK,
                GLOBAL_SESSION_STEP,
                SessionNotificationOperator.REPLACE,
                PROPERTY_STATE,
                STATE_VALUE_STOP
        ));
    }

    public void notifyDecrementSession(String sessionOwner, String session, ProductState oldState) {
        SessionMonitoringEvent event = SessionMonitoringEvent.build(
                sessionOwner,
                session,
                SessionNotificationState.OK,
                GLOBAL_SESSION_STEP,
                SessionNotificationOperator.DEC,
                getSessionProperty(oldState),
                1L
        );
        publisher.publish(event);
    }


    public void notifySipGenerationError(Product product) {
        // Add one to the new state
        SessionMonitoringEvent event = SessionMonitoringEvent.build(
                product.getProcessingChain().getLabel(),
                product.getSession(),
                SessionNotificationState.ERROR,
                GLOBAL_SESSION_STEP,
                SessionNotificationOperator.INC,
                PROPERTY_ERROR,
                1L
        );
        publisher.publish(event);
    }

    public void notifySipGenerationSucceed(Product product) {
        // Decrement the number of sip generated in error, as this one succeed
        if (ProductSIPState.GENERATION_ERROR == product.getSipState()) {
            SessionMonitoringEvent event = SessionMonitoringEvent.build(
                    product.getProcessingChain().getLabel(),
                    product.getSession(),
                    SessionNotificationState.OK,
                    GLOBAL_SESSION_STEP,
                    SessionNotificationOperator.DEC,
                    PROPERTY_GENERATED,
                    1L
            );
            publisher.publish(event);
        }
    }


    private void notifyIncrementSessionError(Product currentProduct) {
        // Add one to the new state
        SessionMonitoringEvent event = SessionMonitoringEvent.build(
                currentProduct.getProcessingChain().getLabel(),
                currentProduct.getSession(),
                SessionNotificationState.ERROR,
                GLOBAL_SESSION_STEP,
                SessionNotificationOperator.INC,
                getSessionProperty(currentProduct.getState()),
                1L
        );
        publisher.publish(event);
    }

    private void notifyIncrementSession(Product currentProduct) {
        // Add one to the new state
        SessionMonitoringEvent event = SessionMonitoringEvent.build(
                currentProduct.getProcessingChain().getLabel(),
                currentProduct.getSession(),
                SessionNotificationState.OK,
                GLOBAL_SESSION_STEP,
                SessionNotificationOperator.INC,
                getSessionProperty(currentProduct.getState()),
                1L
        );
        publisher.publish(event);
    }

    private String getSessionProperty(ProductState state) {
        switch (state) {
            case ACQUIRING:
                return PROPERTY_INCOMPLETE;
            case COMPLETED:
            case FINISHED:
                return PROPERTY_GENERATED;
            case INVALID:
                return PROPERTY_INVALID;
            case UPDATED:
                // Don't need to track such products
        }
        return "";
    }
}
