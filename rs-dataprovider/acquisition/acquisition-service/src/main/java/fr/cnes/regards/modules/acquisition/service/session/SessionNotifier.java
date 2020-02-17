package fr.cnes.regards.modules.acquisition.service.session;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.ingest.domain.sip.ISipState;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionMonitoringEvent;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationOperator;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationState;

/**
 * Service that sends notification to collect statistics about acquisition
 * This service works by session owner, in this service it corresponds to the chain label,
 * and a session name, provided by the user when he starts manually the chain or when the chain starts automatically the current date
 * {@link SessionProductPropertyEnum}
 * <pre>
 *
 *         /    |         \
 *        /     |          \
 *       /      |           \
 * INVALID  INCOMPLETE ___ COMPLETE
 *                            |
 *                            |
 *                            |______ GENERATION_ERROR
 *                            |
 *                         GENERATED
 *                            |
 *                            | ______ INGESTION_FAILED
 *                            |
 *                         INGESTED
 *
 * </pre>
 * @author Léo Mieulet
 * @author Sébastien Binda
 */
@Service
public class SessionNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionNotifier.class);

    /**
     * The name of the property gathering all metadata about this processing step
     */
    private static final String GLOBAL_SESSION_STEP = "dataprovider";

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

    @Autowired
    private IPublisher publisher;

    public void notifyChangeProductState(Product product, ProductState nextState) {
        notifyChangeProductState(product, Optional.of(nextState), Optional.empty());
    }

    public void notifyChangeProductState(Product product, ISipState nextSipState) {
        notifyChangeProductState(product, Optional.empty(), Optional.of(nextSipState));
    }

    public void notifyChangeProductState(Product product, Optional<ProductState> nextState,
            Optional<ISipState> nexSipState) {
        Optional<SessionProductPropertyEnum> current = getProperty(product.getState(), product.getSipState());
        Optional<SessionProductPropertyEnum> next = getProperty(nextState.orElse(product.getState()),
                                                                nexSipState.orElse(product.getSipState()));
        if (!current.equals(next)) {
            notifyDecrementSession(product.getProcessingChain().getLabel(), product.getSession(), product.getState(),
                                   product.getSipState());
            // Add to submitting
            notifyIncrementSession(product.getProcessingChain().getLabel(), product.getSession(),
                                   nextState.orElse(product.getState()), nexSipState.orElse(product.getSipState()));
        }
    }

    public void notifyStartingChain(String sessionOwner, String session) {
        publisher.publish(SessionMonitoringEvent.build(sessionOwner, session, SessionNotificationState.OK,
                                                       GLOBAL_SESSION_STEP, SessionNotificationOperator.REPLACE,
                                                       PROPERTY_STATE, STATE_VALUE_START));
    }

    public void notifyProductDeleted(String sessionOwner, Product product) {
        notifyDecrementSession(sessionOwner, product.getSession(), product.getState(), product.getSipState());
        notifyDecrementSession(sessionOwner, product.getSession(), SessionProductPropertyEnum.PROPERTY_FILES_ACQUIRED,
                               product.getAcquisitionFiles().size());
    }

    public void notifyFileAcquired(String session, String sessionOwner, long nbFilesAcquired) {
        notifyIncrementSession(sessionOwner, session, SessionProductPropertyEnum.PROPERTY_FILES_ACQUIRED,
                               nbFilesAcquired);
    }

    public void notifyChangeProductState(SessionChangingStateProbe probe) {
        // Handle session change
        if (probe.isSessionChanged()) {
            notifyProductChangeSession(probe.getProductName(), probe.getInitialSessionOwner(),
                                       probe.getInitialSession(), probe.getSessionOwner(), probe.getSession(),
                                       probe.getInitialProductState(), probe.getInitialProductSIPState(),
                                       probe.getInitalNbAcquiredFiles());
        }
        // Check if an event must be sent
        if (probe.shouldUpdateState()) {
            // First decrement the old state, if the product existed before and was in the same session
            if ((probe.getInitialProductState() != null) && !probe.isSessionChanged()) {
                notifyDecrementSession(probe.getIngestionChain(), probe.getSession(), probe.getInitialProductState(),
                                       probe.getInitialProductSIPState());
            }
            notifyIncrementSession(probe.getIngestionChain(), probe.getSession(), probe.getProductState(),
                                   probe.getProductSIPState());
        }
    }

    /**
     * Notify session to remove a product and its files to the current session
     * @param label
     * @param product
     */
    public void notifyProductChangeSession(String productName, String sessionOwner, String session,
            String newSessionOwner, String newSession, ProductState productState, ISipState sipState,
            long nbAcquiredFiles) {
        // Decrement number of scanned files
        if (nbAcquiredFiles > 0) {
            notifyDecrementSession(sessionOwner, session, SessionProductPropertyEnum.PROPERTY_FILES_ACQUIRED,
                                   nbAcquiredFiles);
            notifyIncrementSession(newSessionOwner, newSession, SessionProductPropertyEnum.PROPERTY_FILES_ACQUIRED,
                                   nbAcquiredFiles);
        }
        // Decrement from product from previous session
        Optional<SessionProductPropertyEnum> property = getProperty(productState, sipState);
        if (property.isPresent()) {
            notifyDecrementSession(sessionOwner, session, property.get(), 1);
            LOGGER.info("Product {} changed from session {}:{} to session {}:{}. Nb Files switching={}. Old session decrement property : {}",
                        productName, sessionOwner, session, newSessionOwner, newSession, nbAcquiredFiles,
                        property.get().getValue());
        }
    }

    public void notifyEndingChain(String sessionOwner, String session) {
        publisher.publish(SessionMonitoringEvent.build(sessionOwner, session, SessionNotificationState.OK,
                                                       GLOBAL_SESSION_STEP, SessionNotificationOperator.REPLACE,
                                                       PROPERTY_STATE, STATE_VALUE_STOP));
    }

    private void notifyIncrementSession(String sessionOwner, String session, ProductState state, ISipState sipState) {
        Optional<SessionProductPropertyEnum> property = getProperty(state, sipState);
        if (property.isPresent()) {
            LOGGER.trace("Notify increment {}:{} property {}", state.toString(), sipState.toString(),
                         property.get().getValue());
            notifyIncrementSession(sessionOwner, session, property.get());
        }
    }

    private void notifyIncrementSession(String sessionOwner, String session, SessionProductPropertyEnum property) {
        notifyIncrementSession(sessionOwner, session, property, 1L);
    }

    private void notifyIncrementSession(String sessionOwner, String session, SessionProductPropertyEnum property,
            long nbItems) {
        // Add one to the new state
        SessionMonitoringEvent event = SessionMonitoringEvent
                .build(sessionOwner, session, property.getState(), GLOBAL_SESSION_STEP, SessionNotificationOperator.INC,
                       property.getValue(), nbItems);
        publisher.publish(event);
    }

    private void notifyDecrementSession(String sessionOwner, String session, ProductState state, ISipState sipState) {
        Optional<SessionProductPropertyEnum> property = getProperty(state, sipState);
        if (property.isPresent()) {
            LOGGER.trace("Notify decrement {}:{} property {}", state.toString(), sipState.toString(),
                         property.get().getValue());
            notifyDecrementSession(sessionOwner, session, property.get());
        }
    }

    private void notifyDecrementSession(String sessionOwner, String session, SessionProductPropertyEnum property) {
        notifyDecrementSession(sessionOwner, session, property, 1L);
    }

    private void notifyDecrementSession(String sessionOwner, String session, SessionProductPropertyEnum property,
            long nbItems) {
        // Add one to the new state
        SessionMonitoringEvent event = SessionMonitoringEvent
                .build(sessionOwner, session, SessionNotificationState.OK, GLOBAL_SESSION_STEP,
                       SessionNotificationOperator.DEC, property.getValue(), nbItems);
        publisher.publish(event);
    }

    private Optional<SessionProductPropertyEnum> getProperty(ProductState state, ISipState sipState) {
        Optional<SessionProductPropertyEnum> property = getProperty(state);
        // If product is complete so check sip status
        if (property.isPresent() && (property.get() == SessionProductPropertyEnum.PROPERTY_COMPLETED)) {
            Optional<SessionProductPropertyEnum> sipProp = getProperty(sipState);
            // If SIP property is defined return it. Else return product property.
            if (sipProp.isPresent()) {
                property = sipProp;
            }
        }
        return property;
    }

    private Optional<SessionProductPropertyEnum> getProperty(ISipState sipState) {
        Optional<SessionProductPropertyEnum> property = Optional.empty();
        if (sipState == ProductSIPState.SUBMITTED) {
            property = Optional.of(SessionProductPropertyEnum.PROPERTY_GENERATED);
        } else if (sipState == ProductSIPState.GENERATION_ERROR) {
            property = Optional.of(SessionProductPropertyEnum.PROPERTY_GENERATION_ERROR);
        } else if (sipState == ProductSIPState.INGESTION_FAILED) {
            property = Optional.of(SessionProductPropertyEnum.PROPERTY_INGESTION_FAILED);
        } else if (sipState == SIPState.INGESTED) {
            property = Optional.of(SessionProductPropertyEnum.PROPERTY_INGESTED);
        }
        return property;
    }

    private Optional<SessionProductPropertyEnum> getProperty(ProductState state) {
        SessionProductPropertyEnum property;
        switch (state) {
            case ACQUIRING:
                property = SessionProductPropertyEnum.PROPERTY_INCOMPLETE;
                break;
            case COMPLETED:
            case FINISHED:
            case UPDATED:
                property = SessionProductPropertyEnum.PROPERTY_COMPLETED;
                break;
            case INVALID:
                property = SessionProductPropertyEnum.PROPERTY_INVALID;
                break;
            default:
                property = null;
                break;
        }
        return Optional.ofNullable(property);
    }

}