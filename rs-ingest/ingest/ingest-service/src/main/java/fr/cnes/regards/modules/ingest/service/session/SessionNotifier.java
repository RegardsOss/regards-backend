package fr.cnes.regards.modules.ingest.service.session;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.session.agent.client.ISessionAgentClient;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepProperty;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyInfo;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyStateEnum;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.modules.ingest.dao.IAIPPostProcessRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.postprocessing.AIPPostProcessRequest;
import java.util.Collection;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    // Name of the step
    private static final String SESSION_NOTIF_STEP = "oais";

    // Parameters to determine step properties

    public static final String TOTAL_REQUESTS = "total_requests";

    public static final String REQUESTS_ERRORS = "requests_errors";

    public static final String REQUESTS_RUNNING = "requests_running";

    public static final String GENERATED_PRODUCTS = "generated_products";

    public static final String NEW_PRODUCT_VERSIONS = "new_product_versions";

    public static final String REPLACED_PRODUCTS = "replaced_products";

    public static final String IGNORED_PRODUCTS = "ignored_products";

    public static final String PRODUCT_WAIT_VERSION_MODE = "product_wait_version_mode";

    public static final String POST_PROCESS_PENDING = "post_process_pending";

    public static final String POST_PROCESS_SUCCESS = "post_process_success";

    public static final String POST_PROCESS_ERROR = "post_process_error";

    // Request count - corresponds to the number of referencing requests

    public void incrementRequestCount(String source, String session, int nbRequests) {
        incrementCount(source, session, TOTAL_REQUESTS, nbRequests, StepPropertyStateEnum.SUCCESS, true, false);
    }

    public void incrementRequestCount(IngestRequest request) {
        incrementCount(request, TOTAL_REQUESTS, 1, StepPropertyStateEnum.SUCCESS, true, false);
    }

    // AIP generation

    public void incrementProductGenerationPending(IngestRequest request) {
        incrementCount(request, REQUESTS_RUNNING, 1, StepPropertyStateEnum.RUNNING, false, false);
    }

    public void decrementProductGenerationPending(IngestRequest request) {
        decrementCount(request, REQUESTS_RUNNING, 1, StepPropertyStateEnum.RUNNING, false, false);
    }

    public void incrementProductGenerationError(IngestRequest request) {
        incrementCount(request, REQUESTS_ERRORS, 1, StepPropertyStateEnum.ERROR, false, false);
    }

    public void decrementProductGenerationError(IngestRequest request) {
        decrementCount(request, REQUESTS_ERRORS, 1, StepPropertyStateEnum.ERROR, false, false);
    }

    // File storage

    public void incrementProductStorePending(IngestRequest request) {
        incrementCount(request, REQUESTS_RUNNING, request.getAips().size(), StepPropertyStateEnum.RUNNING, false,
                       false);

        //FIXME
       /* // Synchronize number of products according to available AIP(s)
        if (request.getAips().size() > 1) {
            // Increment number of total products (case of one SIP for many AIPs)
            // In this way, there is the same count of products stored and total.
            // 1 request => 1 product, but can produce several AIPs, so we add more product to match the number of AIPs
            incrementCount(request, PRODUCT_COUNT, request.getAips().size() - 1, StepPropertyStateEnum.OK);
        }*/
    }

    public void decrementProductStorePending(IngestRequest request) {
        decrementCount(request, REQUESTS_RUNNING, request.getAips().size(), StepPropertyStateEnum.RUNNING, false,
                       false);
    }

    public void incrementProductStoreSuccess(IngestRequest request) {
        incrementCount(request, GENERATED_PRODUCTS, request.getAips().size(), StepPropertyStateEnum.SUCCESS, false,
                       true);
    }

    public void incrementProductStoreError(IngestRequest request) {
        incrementCount(request, REQUESTS_ERRORS, request.getAips().size(), StepPropertyStateEnum.ERROR, false, false);
    }

    public void decrementProductStoreError(IngestRequest request) {
        decrementCount(request, REQUESTS_ERRORS, request.getAips().size(), StepPropertyStateEnum.ERROR, false, false);
    }

    public void incrementProductIgnored(IngestRequest request) {
        if (request.getState() == InternalRequestState.IGNORED) {
            this.decrementProductGenerationPending(request);
            incrementCount(request, IGNORED_PRODUCTS, 1, StepPropertyStateEnum.INFO, false, false);
        }
    }

    // Product versioning

    public void incrementNewProductVersion(AIPEntity aipEntity) {
        incrementCount(aipEntity, NEW_PRODUCT_VERSIONS, 1, StepPropertyStateEnum.INFO, false, false);
    }

    public void incrementProductReplace(AIPEntity aipEntity) {
        incrementCount(aipEntity, REPLACED_PRODUCTS, 1, StepPropertyStateEnum.INFO, false, false);
    }

    public void incrementProductWaitingVersioningMode(IngestRequest request) {
        if (request.getState() == InternalRequestState.WAITING_VERSIONING_MODE) {
            this.decrementProductGenerationPending(request);
            incrementCount(request, PRODUCT_WAIT_VERSION_MODE, 1, StepPropertyStateEnum.WAITING, false, false);
        }
    }

    public void decrementProductWaitingVersioningMode(IngestRequest request) {
        if (request.getState() == InternalRequestState.WAITING_VERSIONING_MODE) {
            decrementCount(request, PRODUCT_WAIT_VERSION_MODE, 1, StepPropertyStateEnum.WAITING, false, false);
        }
    }

    // Post Process

    public void incrementPostProcessSuccess(AIPPostProcessRequest request) {
        incrementCount(request, POST_PROCESS_SUCCESS, 1, StepPropertyStateEnum.INFO, false, false);
        decrementCount(request, POST_PROCESS_PENDING, 1, StepPropertyStateEnum.INFO, false, false);
    }

    public void incrementPostProcessPending(AIPPostProcessRequest request) {
        incrementCount(request, POST_PROCESS_PENDING, 1, StepPropertyStateEnum.INFO, false, false);
    }

    public void decrementPostProcessPending(AIPPostProcessRequest request) {
        decrementCount(request, POST_PROCESS_PENDING, 1, StepPropertyStateEnum.INFO, false, false);
    }

    public void incrementPostProcessError(AIPPostProcessRequest request) {
        incrementCount(request, POST_PROCESS_ERROR, 1, StepPropertyStateEnum.INFO, false, false);
        decrementCount(request, POST_PROCESS_PENDING, 1, StepPropertyStateEnum.INFO, false, false);
    }

    public void decrementPostProcessError(AIPPostProcessRequest request) {
        decrementCount(request, POST_PROCESS_ERROR, 1, StepPropertyStateEnum.INFO, false, false);
    }

    // AIP storage

    /**
     * Notify session when a request is deleted
     *
     * @param request
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
     * Decrement product store errors or product generation errors
     */
    public void ingestRequestErrorDeleted(IngestRequest request) {
        if (request.getState() == InternalRequestState.ERROR) {
            switch (request.getStep()) {
                case LOCAL_DENIED:
                case LOCAL_FINAL:
                case LOCAL_GENERATION:
                case LOCAL_INIT:
                case LOCAL_POST_PROCESSING:
                case LOCAL_PRE_PROCESSING:
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
            decrementCount(sessionOwner, session, REQUESTS_RUNNING, nbStorePending, StepPropertyStateEnum.RUNNING,
                           false, false);
        }
        if (nbStored > 0) {
            // -x product_stored
            decrementCount(sessionOwner, session, GENERATED_PRODUCTS, nbStored, StepPropertyStateEnum.SUCCESS, false,
                           true);
        }
        decrementCount(sessionOwner, session, GENERATED_PRODUCTS, nbGenerated + nbStored, StepPropertyStateEnum.SUCCESS,
                       true, false);
    }

    // ----------- UTILS -----------

    // GENERIC METHODS TO BUILD NOTIFICATIONS

    // INC
    public void incrementCount(AbstractRequest request, String property, int nbProducts, StepPropertyStateEnum state,
            boolean inputRelated, boolean outputRelated) {
        StepProperty step = new StepProperty(SESSION_NOTIF_STEP, request.getSessionOwner(), request.getSession(),
                                             new StepPropertyInfo(StepTypeEnum.REFERENCING, state, property,
                                                                  String.valueOf(nbProducts), inputRelated,
                                                                  outputRelated));
        sessionNotificationClient.increment(step);
    }

    public void incrementCount(AIPEntity aipEntity, String property, int nbProducts, StepPropertyStateEnum state,
            boolean inputRelated, boolean outputRelated) {
        StepProperty step = new StepProperty(SESSION_NOTIF_STEP, aipEntity.getSessionOwner(), aipEntity.getSession(),
                                             new StepPropertyInfo(StepTypeEnum.REFERENCING, state, property,
                                                                  String.valueOf(nbProducts), inputRelated,
                                                                  outputRelated));
        sessionNotificationClient.increment(step);
    }

    public void incrementCount(String source, String session, String property, int nbProducts,
            StepPropertyStateEnum state, boolean inputRelated, boolean outputRelated) {
        StepProperty step = new StepProperty(SESSION_NOTIF_STEP, source, session,
                                             new StepPropertyInfo(StepTypeEnum.REFERENCING, state, property,
                                                                  String.valueOf(nbProducts), inputRelated,
                                                                  outputRelated));
        sessionNotificationClient.increment(step);
    }

    // DEC
    public void decrementCount(AbstractRequest request, String property, int nbProducts, StepPropertyStateEnum state,
            boolean inputRelated, boolean outputRelated) {
        StepProperty step = new StepProperty(SESSION_NOTIF_STEP, request.getSessionOwner(), request.getSession(),
                                             new StepPropertyInfo(StepTypeEnum.REFERENCING, state, property,
                                                                  String.valueOf(nbProducts), inputRelated,
                                                                  outputRelated));
        sessionNotificationClient.decrement(step);
    }

    public void decrementCount(String source, String session, String property, int nbProducts,
            StepPropertyStateEnum state, boolean inputRelated, boolean outputRelated) {
        StepProperty step = new StepProperty(SESSION_NOTIF_STEP, source, session,
                                             new StepPropertyInfo(StepTypeEnum.REFERENCING, state, property,
                                                                  String.valueOf(nbProducts), inputRelated,
                                                                  outputRelated));
        sessionNotificationClient.decrement(step);
    }
}