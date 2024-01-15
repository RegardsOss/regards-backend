package fr.cnes.regards.modules.acquisition.service.session;

import com.google.common.base.Strings;
import fr.cnes.regards.framework.modules.session.agent.client.ISessionAgentClient;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepProperty;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyInfo;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.ingest.dto.ISipState;
import fr.cnes.regards.modules.ingest.dto.SIPState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

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
     * The name of the property gathering all metadata about this processing step
     */
    public static final String GLOBAL_SESSION_STEP = "scan";

    /**
     * Class LOGGER
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionNotifier.class);

    /**
     * Service to notify changes on steps
     */
    @Autowired
    private ISessionAgentClient notificationClient;

    // CHAIN
    public void notifyStartingChain(String source, String session) {
        notifyIncrementSession(source, session, SessionProductPropertyEnum.CHAIN_RUNNING, 1L);
    }

    public void notifyEndingChain(String source, String session) {
        notifyDecrementSession(source, session, SessionProductPropertyEnum.CHAIN_RUNNING, 1L);
    }

    // PRODUCT

    public void notifyChangeProductState(Product product, ISipState nextSipState, boolean isPreviousStateToDecrement) {
        notifyChangeProductState(product, Optional.of(nextSipState), isPreviousStateToDecrement);
    }

    private void notifyChangeProductState(Product product,
                                          Optional<ISipState> nextSipState,
                                          boolean isPreviousStateToDecrement) {
        Optional<SessionProductPropertyEnum> current = getProperty(product.getState(), product.getSipState());
        Optional<SessionProductPropertyEnum> next = getProperty(product.getState(),
                                                                nextSipState.orElse(product.getSipState()));
        if (!current.equals(next)) {
            if (isPreviousStateToDecrement) {
                notifyDecrementSession(product.getProcessingChain().getLabel(),
                                       product.getSession(),
                                       product.getState(),
                                       product.getSipState());
            }

            // Add to submitting
            notifyIncrementSession(product.getProcessingChain().getLabel(),
                                   product.getSession(),
                                   product.getState(),
                                   nextSipState.orElse(product.getSipState()));
        }
    }

    public void notifyChangeProductState(SessionChangingStateProbe probe) {
        // Check if an event must be sent
        if (probe.shouldUpdateState()) {
            // First decrement the old state, if the product existed before and was in the same session
            if ((probe.getInitialProductState() != null) && !probe.isSessionChanged()) {
                notifyDecrementSession(probe.getIngestionChain(),
                                       probe.getSession(),
                                       probe.getInitialProductState(),
                                       probe.getInitialProductSIPState());
            }
            notifyIncrementSession(probe.getIngestionChain(),
                                   probe.getSession(),
                                   probe.getProductState(),
                                   probe.getProductSIPState());
        }
    }

    public void notifyProductDeleted(String sessionOwner, Product product) {
        String session = product.getSession();
        Optional<SessionProductPropertyEnum> property = notifyDecrementSession(sessionOwner,
                                                                               session,
                                                                               product.getState(),
                                                                               product.getSipState());
    }

    public void notifyFileAcquired(String session, String sessionOwner, long nbFilesAcquired) {
        notifyIncrementSession(sessionOwner,
                               session,
                               SessionProductPropertyEnum.PROPERTY_FILES_ACQUIRED,
                               nbFilesAcquired);
    }

    public void notifyFileAcquiredDeleted(String session, String sessionOwner, int nbFilesAcquired) {
        notifyDecrementSession(sessionOwner,
                               session,
                               SessionProductPropertyEnum.PROPERTY_FILES_ACQUIRED,
                               nbFilesAcquired);
    }

    public void notifyFileInvalid(String session, String sessionOwner, long nbFilesAcquired) {
        notifyIncrementSession(sessionOwner,
                               session,
                               SessionProductPropertyEnum.PROPERTY_FILES_INVALID,
                               nbFilesAcquired);
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

    /**
     * Send an INC event to {@link ISessionAgentClient}
     *
     * @param source   also called sessionOwner, originator of the request
     * @param session  tags the data processed with the same name
     * @param property property to be notified
     * @param nbItems  value to increment the corresponding property
     */
    private void notifyIncrementSession(String source,
                                        String session,
                                        SessionProductPropertyEnum property,
                                        long nbItems) {
        if (!Strings.isNullOrEmpty(source) && !Strings.isNullOrEmpty(session)) {
            StepProperty step = new StepProperty(GLOBAL_SESSION_STEP,
                                                 source,
                                                 session,
                                                 new StepPropertyInfo(StepTypeEnum.ACQUISITION,
                                                                      property.getState(),
                                                                      property.getName(),
                                                                      String.valueOf(nbItems),
                                                                      property.isInputRelated(),
                                                                      property.isOutputRelated()));
            notificationClient.increment(step);
        } else {
            LOGGER.debug(
                "Session has not been incremented of {} items because either sessionOwner({}) or session({}) is null or empty",
                nbItems,
                source,
                session);
        }
    }

    // DEC

    private Optional<SessionProductPropertyEnum> notifyDecrementSession(String sessionOwner,
                                                                        String session,
                                                                        ProductState state,
                                                                        ISipState sipState) {
        Optional<SessionProductPropertyEnum> property = getProperty(state, sipState);
        if (property.isPresent()) {
            LOGGER.trace("Notify decrement {}:{} property {}", state, sipState, property.get().getName());
            notifyDecrementSession(sessionOwner, session, property.get(), 1L);
        }
        return property;
    }

    /**
     * Send an DEC event to {@link ISessionAgentClient}
     *
     * @param source   also called sessionOwner, originator of the request
     * @param session  tags the data processed with the same name
     * @param property property to be notified
     * @param nbItems  value to decrement the corresponding property
     */
    private void notifyDecrementSession(String source,
                                        String session,
                                        SessionProductPropertyEnum property,
                                        long nbItems) {
        if (!Strings.isNullOrEmpty(source) && !Strings.isNullOrEmpty(session)) {
            StepProperty step = new StepProperty(GLOBAL_SESSION_STEP,
                                                 source,
                                                 session,
                                                 new StepPropertyInfo(StepTypeEnum.ACQUISITION,
                                                                      property.getState(),
                                                                      property.getName(),
                                                                      String.valueOf(nbItems),
                                                                      property.isInputRelated(),
                                                                      property.isOutputRelated()));
            notificationClient.decrement(step);
            // If property to decrement is ingested, decrement generated too.
            if (property == SessionProductPropertyEnum.PROPERTY_INGESTED) {
                SessionProductPropertyEnum generatedProperty = SessionProductPropertyEnum.PROPERTY_GENERATED_PRODUCTS;
                step = new StepProperty(GLOBAL_SESSION_STEP,
                                        source,
                                        session,
                                        new StepPropertyInfo(StepTypeEnum.ACQUISITION,
                                                             generatedProperty.getState(),
                                                             generatedProperty.getName(),
                                                             String.valueOf(nbItems),
                                                             generatedProperty.isInputRelated(),
                                                             generatedProperty.isOutputRelated()));
                notificationClient.decrement(step);
            }

        } else {
            LOGGER.debug("Session has not been decremented of {} items because either sessionOwner({}) or session({}) "
                         + "is null or empty", nbItems, source, session);
        }
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
            property = Optional.of(SessionProductPropertyEnum.PROPERTY_GENERATED_PRODUCTS);
        } else if (sipState == ProductSIPState.GENERATION_ERROR) {
            property = Optional.of(SessionProductPropertyEnum.PROPERTY_GENERATION_ERROR);
        } else if (sipState == ProductSIPState.INGESTION_FAILED) {
            property = Optional.of(SessionProductPropertyEnum.PROPERTY_INGESTION_FAILED);
        } else if (sipState == SIPState.INGESTED) {
            property = Optional.of(SessionProductPropertyEnum.PROPERTY_INGESTED);
        } else if (sipState == SIPState.DELETED) {
            property = Optional.of(SessionProductPropertyEnum.PROPERTY_INGESTION_CANCELED);
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
            case CANCELED:
                property = SessionProductPropertyEnum.PROPERTY_INGESTION_CANCELED;
                break;
            default:
                property = null;
                break;
        }
        return Optional.ofNullable(property);
    }
}