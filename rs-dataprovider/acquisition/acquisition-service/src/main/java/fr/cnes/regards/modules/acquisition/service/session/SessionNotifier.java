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
 * Service that sends notification to collect stats about acquisition
 * This service works by session owner, in this µservice it corresponds to the chain label,
 * and a session name, provided by the user when he starts manually the chain or when the chain starts automatically the current date
 *
 * <pre>
 *
 *          INCOMPLETE
 *             |
 *          GENERATED
 *             |
 *             | _________ INVALID or GENERATION_ERROR
 *             |
 *         SUBMITTED
 *             | _________ INGESTION_FAILED
 *             |
 *          INGESTED
 *
 * </pre>
 * @author Léo Mieulet
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

    /**
     * Name of the property that collects number of products generated
     */
    public static final String PROPERTY_GENERATED = "generated";

    /**
     * Name of the property that collects number of products incomplete
     */
    public static final String PROPERTY_INCOMPLETE = "incomplete";

    /**
     * Name of the property that collects number of products invalid (too many files attached to a single product)
     */
    public static final String PROPERTY_INVALID = "invalid";

    /**
     * Name of the property that collects number of products generated
     */
    public static final String PROPERTY_GENERATION_ERROR = "generation_error";

    /**
     * Name of the property that collects number of products sent to INGEST
     */
    public static final String PROPERTY_SUBMITTED = "submitted";

    public static final String PROPERTY_INGESTION_FAILED = "ingestion_failed";

    public static final String PROPERTY_INGESTED = "ingested";

    public static final String PROPERTY_FILES_ACQUIRED = "files_acquired";

    @Autowired
    private IPublisher publisher;

    public void notifyProductDeleted(String sessionOwner, Product product) {
        notifyDecrementSession(sessionOwner, product.getSession(), product.getState());
        notifyDecrementSession(sessionOwner, product.getSession(), PROPERTY_FILES_ACQUIRED, SessionNotificationState.OK,
                               product.getAcquisitionFiles().size());
    }

    public void notifyFileAcquired(String session, String sessionOwner, long nbFilesAcquired) {
        notifyIncrementSession(sessionOwner, session, PROPERTY_FILES_ACQUIRED, SessionNotificationState.OK,
                               nbFilesAcquired);
    }

    public void notifyProductStateChange(SessionChangingStateProbe probe) {
        // Handle session change
        if (probe.isSessionChanged()) {
            notifyProductChangeSession(probe.getProductName(), probe.getInitialSessionOwner(),
                                       probe.getInitialSession(), probe.getSessionOwner(), probe.getSession(),
                                       probe.getInitialProductState(), probe.getInitialProductSIPState(),
                                       probe.getInitalNbAcquiredFiles());
        }
        // Check if an event must be sent
        if (probe.shouldUpdateState()) {
            // First decrement the old state, if the product existed before
            if ((probe.getInitialProductState() != null) && !probe.isSessionChanged()) {
                notifyDecrementSession(probe.getIngestionChain(), probe.getSession(), probe.getInitialProductState());
            }
            // Increment the new state
            switch (probe.getProductState()) {
                case ACQUIRING:
                case COMPLETED:
                case FINISHED:
                    notifyIncrementSession(probe.getIngestionChain(), probe.getSession(), probe.getProductState());
                    break;
                case INVALID:
                    notifyIncrementSessionWithError(probe.getIngestionChain(), probe.getSession(),
                                                    probe.getProductState());
                    break;
                case UPDATED:
                    // Don't need to track such products
            }
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
            notifyDecrementSession(sessionOwner, session, PROPERTY_FILES_ACQUIRED, SessionNotificationState.OK,
                                   nbAcquiredFiles);
            notifyIncrementSession(newSessionOwner, newSession, PROPERTY_FILES_ACQUIRED, SessionNotificationState.OK,
                                   nbAcquiredFiles);
        }
        // Decrement from product state
        Optional<String> productStateProp = getSessionProperty(productState);
        if (productStateProp.isPresent()) {
            notifyDecrementSession(sessionOwner, session, productStateProp.get(), SessionNotificationState.OK, 1);
        }
        // Decrement from sip state
        Optional<String> sipStateProp = getProductStateProperty(sipState);
        if (sipStateProp.isPresent()) {
            notifyDecrementSession(sessionOwner, session, sipStateProp.get(), SessionNotificationState.OK, 1);
        }
        LOGGER.info("Product {} changed from session {}:{} to session {}:{}. Nb Files switching={}. Old session decrement properties : {},{}",
                    productName, sessionOwner, session, newSessionOwner, newSession, nbAcquiredFiles,
                    productStateProp.orElse(""), sipStateProp.orElse(""));
    }

    public void notifySipSubmitting(Product product) {
        // Add a submitting
        notifyIncrementSession(product.getProcessingChain().getLabel(), product.getSession(), product.getSipState());
    }

    public void notifySipGenerationFailed(Product product) {
        // Remove one generated
        notifyDecrementSession(product.getProcessingChain().getLabel(), product.getSession(), PROPERTY_GENERATED);

        // Add a submitting
        notifyIncrementSessionWithError(product.getProcessingChain().getLabel(), product.getSession(),
                                        product.getSipState());
    }

    public void notifyRelaunchSIPGeneration(Product product) {
        // This method is intented to reset from INVALID or GENERATION_ERROR to GENERATED
        if (product.getSipState() != ProductSIPState.NOT_SCHEDULED) {
            // Remove a product from errors
            notifyDecrementSession(product.getProcessingChain().getLabel(), product.getSession(),
                                   product.getSipState());
            // Add to submitting
            notifyIncrementSession(product.getProcessingChain().getLabel(), product.getSession(),
                                   ProductState.COMPLETED);
        }
    }

    public void notifyProductIngested(Product product) {
        // Remove a product in submitting
        notifyDecrementSession(product.getProcessingChain().getLabel(), product.getSession(),
                               ProductSIPState.SUBMITTED);
        // Add to submitting
        notifyIncrementSession(product.getProcessingChain().getLabel(), product.getSession(), SIPState.INGESTED);
    }

    public void notifyProductIngestFailure(Product product) {
        // Remove a product in submitting
        notifyDecrementSession(product.getProcessingChain().getLabel(), product.getSession(),
                               ProductSIPState.SUBMITTED);
        // Add to ingest failure
        notifyIncrementSessionWithError(product.getProcessingChain().getLabel(), product.getSession(),
                                        product.getSipState());
    }

    public void notifyStartingChain(String sessionOwner, String session) {
        publisher.publish(SessionMonitoringEvent.build(sessionOwner, session, SessionNotificationState.OK,
                                                       GLOBAL_SESSION_STEP, SessionNotificationOperator.REPLACE,
                                                       PROPERTY_STATE, STATE_VALUE_START));
    }

    public void notifyEndingChain(String sessionOwner, String session) {
        publisher.publish(SessionMonitoringEvent.build(sessionOwner, session, SessionNotificationState.OK,
                                                       GLOBAL_SESSION_STEP, SessionNotificationOperator.REPLACE,
                                                       PROPERTY_STATE, STATE_VALUE_STOP));
    }

    private void notifyIncrementSessionWithError(String sessionOwner, String session, ISipState sipState) {
        Optional<String> prop = getProductStateProperty(sipState);
        if (prop.isPresent()) {
            notifyIncrementSession(sessionOwner, session, prop.get(), SessionNotificationState.ERROR, 1L);
        }
    }

    private void notifyIncrementSession(String sessionOwner, String session, ISipState sipState) {
        Optional<String> prop = getProductStateProperty(sipState);
        if (prop.isPresent()) {
            notifyIncrementSession(sessionOwner, session, prop.get());
        }
    }

    private void notifyIncrementSessionWithError(String sessionOwner, String session, ProductState property) {
        Optional<String> prop = getSessionProperty(property);
        if (prop.isPresent()) {
            notifyIncrementSession(sessionOwner, session, prop.get(), SessionNotificationState.ERROR, 1L);
        }
    }

    private void notifyIncrementSession(String sessionOwner, String session, ProductState property) {
        Optional<String> prop = getSessionProperty(property);
        if (prop.isPresent()) {
            notifyIncrementSession(sessionOwner, session, prop.get());
        }
    }

    private void notifyIncrementSession(String sessionOwner, String session, String property) {
        notifyIncrementSession(sessionOwner, session, property, SessionNotificationState.OK, 1L);
    }

    private void notifyIncrementSession(String sessionOwner, String session, String property,
            SessionNotificationState notifState, long nbItems) {
        // Add one to the new state
        SessionMonitoringEvent event = SessionMonitoringEvent.build(sessionOwner, session, notifState,
                                                                    GLOBAL_SESSION_STEP,
                                                                    SessionNotificationOperator.INC, property, nbItems);
        publisher.publish(event);
    }

    private void notifyDecrementSession(String sessionOwner, String session, ISipState sipState) {
        Optional<String> prop = getProductStateProperty(sipState);
        if (prop.isPresent()) {
            notifyDecrementSession(sessionOwner, session, prop.get());
        }
    }

    private void notifyDecrementSession(String sessionOwner, String session, ProductState property) {
        Optional<String> prop = getSessionProperty(property);
        if (prop.isPresent()) {
            notifyDecrementSession(sessionOwner, session, prop.get());
        }
    }

    private void notifyDecrementSession(String sessionOwner, String session, String property) {
        notifyDecrementSession(sessionOwner, session, property, SessionNotificationState.OK, 1L);
    }

    private void notifyDecrementSession(String sessionOwner, String session, String property,
            SessionNotificationState notifState, long nbItems) {
        // Add one to the new state
        SessionMonitoringEvent event = SessionMonitoringEvent.build(sessionOwner, session, notifState,
                                                                    GLOBAL_SESSION_STEP,
                                                                    SessionNotificationOperator.DEC, property, nbItems);
        publisher.publish(event);
    }

    private Optional<String> getProductStateProperty(ISipState sipState) {
        if (sipState == ProductSIPState.SUBMITTED) {
            return Optional.of(PROPERTY_SUBMITTED);
        } else if (sipState == ProductSIPState.GENERATION_ERROR) {
            return Optional.of(PROPERTY_GENERATION_ERROR);
        } else if (sipState == ProductSIPState.INGESTION_FAILED) {
            return Optional.of(PROPERTY_INGESTION_FAILED);
        } else if (sipState == SIPState.INGESTED) {
            return Optional.of(PROPERTY_INGESTED);
        }
        return Optional.empty();
    }

    private Optional<String> getSessionProperty(ProductState state) {
        switch (state) {
            case ACQUIRING:
                return Optional.of(PROPERTY_INCOMPLETE);
            case COMPLETED:
            case FINISHED:
                return Optional.of(PROPERTY_GENERATED);
            case INVALID:
                return Optional.of(PROPERTY_INVALID);
            case UPDATED:
                // Don't need to track such products
        }
        return Optional.empty();
    }

}