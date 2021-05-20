package fr.cnes.regards.modules.acquisition.service.session;

import fr.cnes.regards.framework.modules.session.agent.client.ISessionAgentClient;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepProperty;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyInfo;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.ingest.domain.sip.ISipState;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
 *
 * @author Léo Mieulet
 * @author Sébastien Binda
 */
@Service
public class SessionNotifier {

    /**
     * Service to notify changes on steps
     */
    @Autowired
    private ISessionAgentClient notificationClient;

    /**
     * Class LOGGER
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionNotifier.class);

    /**
     * The name of the property gathering all metadata about this processing step
     */
    public static final String GLOBAL_SESSION_STEP = "scan";


    // CHAIN
    public void notifyStartingChain(String source, String session) {
        notifyIncrementSession(source, session, SessionProductPropertyEnum.CHAIN_RUNNING, 1L);
    }

    public void notifyEndingChain(String source, String session) {
        notifyDecrementSession(source, session, SessionProductPropertyEnum.CHAIN_RUNNING, 1L);
    }

    // PRODUCT

    public void notifyChangeProductState(Product product, ISipState nextSipState) {
        notifyChangeProductState(product, Optional.of(nextSipState));
    }

    private void notifyChangeProductState(Product product, Optional<ISipState> nextSipState) {
        Optional<SessionProductPropertyEnum> current = getProperty(product.getState(), product.getSipState());
        Optional<SessionProductPropertyEnum> next = getProperty(product.getState(),
                                                                nextSipState.orElse(product.getSipState()));
        if (!current.equals(next)) {
            notifyDecrementSession(product.getProcessingChain().getLabel(), product.getSession(), product.getState(),
                                   product.getSipState());
            // Add to submitting
            notifyIncrementSession(product.getProcessingChain().getLabel(), product.getSession(), product.getState(),
                                   nextSipState.orElse(product.getSipState()));
        }
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

    public void notifyProductDeleted(String sessionOwner, Product product) {
        notifyDecrementSession(sessionOwner, product.getSession(), product.getState(), product.getSipState());
        notifyDecrementSession(sessionOwner, product.getSession(), SessionProductPropertyEnum.PROPERTY_FILES_ACQUIRED,
                               product.getActiveAcquisitionFiles().size());
    }

    public void notifyFileAcquired(String session, String sessionOwner, long nbFilesAcquired) {
        notifyIncrementSession(sessionOwner, session, SessionProductPropertyEnum.PROPERTY_FILES_ACQUIRED,
                               nbFilesAcquired);
    }

    /**
     * Notify session to remove a product and its files to the current session
     */
    private void notifyProductChangeSession(String productName, String sessionOwner, String session,
            String newSessionOwner, String newSession, ProductState productState, ISipState sipState,
            long nbAcquiredFiles) {
        // Decrement number of scanned files
        if (nbAcquiredFiles > 0) {
            notifyDecrementSession(sessionOwner, session, SessionProductPropertyEnum.PROPERTY_FILES_ACQUIRED,
                                   nbAcquiredFiles);
            //FIXME: there is a possible double increment with classic process that scan new files using notifyFileAcquired
            // This is not fixed because PM65 should change everything. Be careful with the new way.
            // possible part of solution : remove the following line

            // notifyIncrementSession(newSessionOwner, newSession, SessionProductPropertyEnum.PROPERTY_FILES_ACQUIRED,
            // nbAcquiredFiles);
        }
        // Decrement from product from previous session
        Optional<SessionProductPropertyEnum> property = getProperty(productState, sipState);
        if (property.isPresent()) {
            notifyDecrementSession(sessionOwner, session, property.get(), 1L);
            LOGGER.info(
                    "Product {} changed from session {}:{} to session {}:{}. Nb Files switching={}. Old session decrement property : {}",
                    productName, sessionOwner, session, newSessionOwner, newSession, nbAcquiredFiles,
                    property.get().getName());
        }
    }

    // ----------- UTILS -----------

    // GENERIC METHODS TO BUILD NOTIFICATIONS

    // INC

    private void notifyIncrementSession(String sessionOwner, String session, ProductState state, ISipState sipState) {
        Optional<SessionProductPropertyEnum> property = getProperty(state, sipState);
        if (property.isPresent()) {
            LOGGER.trace("Notify increment {}:{} property {}", state, sipState, property.get().getName());
            notifyIncrementSession(sessionOwner, session, property.get(), 1L);
        }
    }

    private void notifyIncrementSession(String source, String session, SessionProductPropertyEnum property,
            long nbItems) {
        StepProperty step = new StepProperty(GLOBAL_SESSION_STEP, source, session,
                                             new StepPropertyInfo(StepTypeEnum.ACQUISITION, property.getState(),
                                                                  property.getName(), String.valueOf(nbItems),
                                                                  property.isInputRelated(),
                                                                  property.isOutputRelated()));
        notificationClient.increment(step);
    }

    // DEC

    private void notifyDecrementSession(String sessionOwner, String session, ProductState state, ISipState sipState) {
        Optional<SessionProductPropertyEnum> property = getProperty(state, sipState);
        if (property.isPresent()) {
            LOGGER.trace("Notify decrement {}:{} property {}", state, sipState, property.get().getName());
            notifyDecrementSession(sessionOwner, session, property.get(), 1L);
        }
    }

    private void notifyDecrementSession(String source, String session, SessionProductPropertyEnum property,
            long nbItems) {
        StepProperty step = new StepProperty(GLOBAL_SESSION_STEP, source, session,
                                             new StepPropertyInfo(StepTypeEnum.ACQUISITION, property.getState(),
                                                                  property.getName(), String.valueOf(nbItems),
                                                                  property.isInputRelated(),
                                                                  property.isOutputRelated()));
        notificationClient.decrement(step);
    }


    // PROPERTY UTILS

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
            property = Optional.of(SessionProductPropertyEnum.GENERATED_PRODUCTS);
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