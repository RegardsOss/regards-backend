package fr.cnes.regards.modules.ingest.service.session;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.session.agent.client.ISessionAgentClient;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepProperty;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyInfo;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.modules.ingest.dao.IAIPPostProcessRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.postprocessing.AIPPostProcessRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Service
@MultitenantTransactional
public class SessionNotifier {

    /**
     * Repositories
     */
    @Autowired
    private IIngestRequestRepository ingestRequestRepository;

    @Autowired
    private IAIPPostProcessRequestRepository aipPostProcessRequestRepository;

    /**
     * Service to notify changes on steps
     */
    @Autowired
    private ISessionAgentClient sessionNotificationClient;

    /**
     * Parameters
     */
    // Name of the global step
    private static final String GLOBAL_SESSION_STEP = "oais";

    /**
     * Methods used to notify the session
     */
    // Request count - corresponds to the number of referencing requests
    public void incrementRequestCount(String source, String session, int nbRequests) {
        incrementCount(source, session, SessionNotifierPropertyEnum.TOTAL_REQUESTS, nbRequests);
    }

    public void incrementRequestCount(IngestRequest request) {
        incrementCount(request, SessionNotifierPropertyEnum.TOTAL_REQUESTS, 1);
    }

    public void decrementRequestCount(IngestRequest request) {
        decrementCount(request, SessionNotifierPropertyEnum.TOTAL_REQUESTS, 1);
    }

    // AIP generation

    public void incrementProductGenerationPending(IngestRequest request) {
        incrementCount(request, SessionNotifierPropertyEnum.REQUESTS_RUNNING, 1);
    }

    public void decrementProductGenerationPending(IngestRequest request) {
        decrementCount(request, SessionNotifierPropertyEnum.REQUESTS_RUNNING, 1);
    }

    public void incrementProductGenerationError(IngestRequest request) {
        incrementCount(request, SessionNotifierPropertyEnum.REQUESTS_ERRORS, 1);
    }

    public void decrementProductGenerationError(IngestRequest request) {
        decrementCount(request, SessionNotifierPropertyEnum.REQUESTS_ERRORS, 1);
    }

    // File storage

    public void incrementProductStorePending(IngestRequest request) {
        incrementCount(request, SessionNotifierPropertyEnum.REQUESTS_RUNNING, 1);
    }

    public void decrementProductStorePending(IngestRequest request) {
        decrementCount(request, SessionNotifierPropertyEnum.REQUESTS_RUNNING, 1);
    }

    public void incrementProductStoreSuccess(IngestRequest request) {
        incrementCount(request, SessionNotifierPropertyEnum.REFERENCED_PRODUCTS, request.getAips().size());
    }

    public void incrementProductStoreError(IngestRequest request) {
        incrementCount(request, SessionNotifierPropertyEnum.REQUESTS_ERRORS, 1);
    }

    public void decrementProductStoreError(IngestRequest request) {
        decrementCount(request, SessionNotifierPropertyEnum.REQUESTS_ERRORS, 1);
    }

    public void incrementProductIgnored(IngestRequest request) {
        if (request.getState() == InternalRequestState.IGNORED) {
            this.decrementProductGenerationPending(request);
            incrementCount(request, SessionNotifierPropertyEnum.IGNORED_PRODUCTS, 1);
        }
    }

    // Product versioning

    public void incrementNewProductVersion(AIPEntity aipEntity) {
        incrementCount(aipEntity, SessionNotifierPropertyEnum.NEW_PRODUCT_VERSIONS, 1);
    }

    public void incrementProductReplace(AIPEntity aipEntity) {
        incrementCount(aipEntity, SessionNotifierPropertyEnum.REPLACED_PRODUCTS, 1);
    }

    public void incrementProductWaitingVersioningMode(IngestRequest request) {
        if (request.getState() == InternalRequestState.WAITING_VERSIONING_MODE) {
            this.decrementProductGenerationPending(request);
            incrementCount(request, SessionNotifierPropertyEnum.PRODUCT_WAIT_VERSION_MODE, 1);
        }
    }

    public void decrementProductWaitingVersioningMode(IngestRequest request) {
        if (request.getState() == InternalRequestState.WAITING_VERSIONING_MODE) {
            decrementCount(request, SessionNotifierPropertyEnum.PRODUCT_WAIT_VERSION_MODE, 1);
        }
    }

    // Post Process

    public void incrementPostProcessSuccess(AIPPostProcessRequest request) {
        incrementCount(request, SessionNotifierPropertyEnum.POST_PROCESS_SUCCESS, 1);
        decrementCount(request, SessionNotifierPropertyEnum.POST_PROCESS_PENDING, 1);
    }

    public void incrementPostProcessPending(AIPPostProcessRequest request) {
        incrementCount(request, SessionNotifierPropertyEnum.POST_PROCESS_PENDING, 1);
    }

    public void decrementPostProcessPending(AIPPostProcessRequest request) {
        decrementCount(request, SessionNotifierPropertyEnum.POST_PROCESS_PENDING, 1);
    }

    public void incrementPostProcessError(AIPPostProcessRequest request) {
        incrementCount(request, SessionNotifierPropertyEnum.POST_PROCESS_ERROR, 1);
        decrementCount(request, SessionNotifierPropertyEnum.POST_PROCESS_PENDING, 1);
    }

    public void decrementPostProcessError(AIPPostProcessRequest request) {
        decrementCount(request, SessionNotifierPropertyEnum.POST_PROCESS_ERROR, 1);
    }

    // AIP storage

    /**
     * Notify session when a request is deleted
     */
    public void requestDeleted(AbstractRequest request) {
        // If INGEST request
        if (request instanceof IngestRequest) {
            // Load with AIPs
            Optional<IngestRequest> oReq = ingestRequestRepository.findById(request.getId());
            if (oReq.isPresent()) {
                // If request is in error status then we can decrement the number of generation error
                if (request.getState() == InternalRequestState.ERROR) {
                    ingestRequestErrorDeleted(oReq.get());
                } else if (request.getState() == InternalRequestState.WAITING_VERSIONING_MODE) {
                    decrementProductWaitingVersioningMode(oReq.get());
                }
            }
        } else if (request instanceof AIPPostProcessRequest) {
            Optional<AIPPostProcessRequest> oReq = aipPostProcessRequestRepository.findById(request.getId());
            if (oReq.isPresent()) {
                if (request.getState() == InternalRequestState.ERROR) {
                    decrementPostProcessError(oReq.get());
                } else if (request.getState() == InternalRequestState.WAITING_VERSIONING_MODE) {
                    decrementPostProcessPending(oReq.get());
                }
            }
        }
    }

    /**
     * Decrement product request error and one request from the total request
     */
    public void ingestRequestErrorDeleted(IngestRequest request) {
        decrementRequestCount(request);
        decrementProductRequestError(request);
    }

    /**
     * Decrement product store error or product generation error
     */
    public void decrementProductRequestError(IngestRequest request) {
        if (request.getState() == InternalRequestState.ERROR) {
            switch (request.getStep()) {
                case LOCAL_DENIED:
                case LOCAL_FINAL:
                case LOCAL_GENERATION:
                case LOCAL_INIT:
                case LOCAL_POST_PROCESSING:
                case LOCAL_PRE_PROCESSING:
                case LOCAL_AIP_STORAGE_METADATA_UPDATE:
                case LOCAL_SCHEDULED:
                case LOCAL_TAGGING:
                case LOCAL_VALIDATION:
                    decrementProductGenerationError(request);
                    break;
                case REMOTE_STORAGE_REQUESTED:
                case REMOTE_STORAGE_DENIED:
                case REMOTE_STORAGE_ERROR:
                    decrementProductStoreError(request);
                    break;
                case LOCAL_TO_BE_NOTIFIED:
                case REMOTE_NOTIFICATION_ERROR:
                    // Nothing to do, Notification step failed, product is correctly stored. Session is not impacted
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Notify session that a product is deleted
     *
     * @param sessionOwner
     * @param session
     * @param aips         {@link AIPEntity}s of the deleted product
     */
    public void productDeleted(String sessionOwner, String session, Collection<AIPEntity> aips) {
        int nbGenerated = 0;
        int nbStored = 0;
        int nbStorePending = 0;
        for (AIPEntity aip : aips) {
            // Check if an ingest request exists in error status for the given AIP. If exists, so the product is not
            // referenced as store pending in the session but as store error.
            switch (aip.getState()) {
                case GENERATED:
                    if (!ingestRequestRepository.existsByAipsIdAndState(aip.getId(), InternalRequestState.ERROR)) {
                        nbStorePending++;
                    }
                    nbGenerated++;
                    break;
                case STORED:
                    nbStored++;
                    break;
                case DELETED:
                default:
                    break;
            }
        }
        if ((nbStorePending > 0)) {
            // -x product_storing
            decrementCount(sessionOwner, session, SessionNotifierPropertyEnum.REQUESTS_RUNNING, nbStorePending);
        }

        int nbDeletedProducts = nbGenerated + nbStored;
        decrementCount(sessionOwner, session, SessionNotifierPropertyEnum.REFERENCED_PRODUCTS, nbDeletedProducts);
        incrementCount(sessionOwner, session, SessionNotifierPropertyEnum.DELETED_PRODUCTS, nbDeletedProducts);
    }

    // ----------- UTILS -----------

    // GENERIC METHODS TO BUILD NOTIFICATIONS

    // INC
    public void incrementCount(AbstractRequest request, SessionNotifierPropertyEnum property, int nbProducts) {
        StepProperty step = new StepProperty(GLOBAL_SESSION_STEP,
                                             request.getSessionOwner(),
                                             request.getSession(),
                                             new StepPropertyInfo(StepTypeEnum.REFERENCING,
                                                                  property.getState(),
                                                                  property.getName(),
                                                                  String.valueOf(nbProducts),
                                                                  property.isInputRelated(),
                                                                  property.isOutputRelated()));
        sessionNotificationClient.increment(step);
    }

    public void incrementCount(AIPEntity aipEntity, SessionNotifierPropertyEnum property, int nbProducts) {
        StepProperty step = new StepProperty(GLOBAL_SESSION_STEP,
                                             aipEntity.getSessionOwner(),
                                             aipEntity.getSession(),
                                             new StepPropertyInfo(StepTypeEnum.REFERENCING,
                                                                  property.getState(),
                                                                  property.getName(),
                                                                  String.valueOf(nbProducts),
                                                                  property.isInputRelated(),
                                                                  property.isOutputRelated()));
        sessionNotificationClient.increment(step);
    }

    public void incrementCount(String source, String session, SessionNotifierPropertyEnum property, int nbProducts) {
        StepProperty step = new StepProperty(GLOBAL_SESSION_STEP,
                                             source,
                                             session,
                                             new StepPropertyInfo(StepTypeEnum.REFERENCING,
                                                                  property.getState(),
                                                                  property.getName(),
                                                                  String.valueOf(nbProducts),
                                                                  property.isInputRelated(),
                                                                  property.isOutputRelated()));
        sessionNotificationClient.increment(step);
    }

    // DEC
    public void decrementCount(AbstractRequest request, SessionNotifierPropertyEnum property, int nbProducts) {
        StepProperty step = new StepProperty(GLOBAL_SESSION_STEP,
                                             request.getSessionOwner(),
                                             request.getSession(),
                                             new StepPropertyInfo(StepTypeEnum.REFERENCING,
                                                                  property.getState(),
                                                                  property.getName(),
                                                                  String.valueOf(nbProducts),
                                                                  property.isInputRelated(),
                                                                  property.isOutputRelated()));
        sessionNotificationClient.decrement(step);
    }

    public void decrementCount(String source, String session, SessionNotifierPropertyEnum property, int nbProducts) {
        StepProperty step = new StepProperty(GLOBAL_SESSION_STEP,
                                             source,
                                             session,
                                             new StepPropertyInfo(StepTypeEnum.REFERENCING,
                                                                  property.getState(),
                                                                  property.getName(),
                                                                  String.valueOf(nbProducts),
                                                                  property.isInputRelated(),
                                                                  property.isOutputRelated()));
        sessionNotificationClient.decrement(step);
    }
}