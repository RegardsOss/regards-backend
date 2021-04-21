package fr.cnes.regards.modules.ingest.service.session;

import java.util.Collection;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.ingest.dao.IAIPPostProcessRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.postprocessing.AIPPostProcessRequest;
import fr.cnes.regards.modules.sessionmanager.client.ISessionNotificationClient;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationState;

@Service
@MultitenantTransactional
public class SessionNotifier {

    public static final String PRODUCT_COUNT = "products";

    public static final String PRODUCT_GEN_ERROR = "products_gen_error";

    public static final String PRODUCT_GEN_PENDING = "products_gen_pending";

    public static final String PRODUCT_STORE_PENDING = "products_store_pending";

    public static final String PRODUCT_STORED = "products_stored";

    public static final String POST_PROCESS_PENDING = "post_process_pending";

    public static final String POST_PROCESS_ERROR = "post_process_error";

    public static final String POST_PROCESS_SUCCESS = "post_process_success";

    public static final String PRODUCT_STORE_ERROR = "products_store_error";

    public static final String PRODUCT_IGNORED = "products_ignored";

    public static final String PRODUCT_WAITING_VERSIONING_MODE = "products_waiting_versioning_mode";

    public static final String PRODUCT_REPLACED = "products_replaced";

    public static final String NEW_PRODUCT_VERSIONS = "new_product_versions";

    private static final String SESSION_NOTIF_STEP = "oais";

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionNotifier.class);

    @Autowired
    private ISessionNotificationClient sessionNotificationClient;

    @Autowired
    private IIngestRequestRepository ingestRequestRepository;

    @Autowired
    private IAIPPostProcessRequestRepository aipPostProcessRequestRepository;

    @PostConstruct
    public void init() {
        sessionNotificationClient.setStep(SESSION_NOTIF_STEP);
    }

    // Product count

    public void incrementProductCount(IngestRequest request, int nbProducts) {
        sessionNotificationClient.increment(request.getSessionOwner(), request.getSession(), PRODUCT_COUNT,
                                  SessionNotificationState.OK, nbProducts);
    }

    public void incrementProductCount(IngestRequest request) {
        incrementProductCount(request, 1);
    }

    public void decrementProductCount(IngestRequest request) {
        sessionNotificationClient.decrement(request.getSessionOwner(), request.getSession(), PRODUCT_COUNT,
                                  SessionNotificationState.OK, 1);
    }

    // AIP generation

    public void incrementProductGenerationPending(IngestRequest request) {
        sessionNotificationClient.increment(request.getSessionOwner(), request.getSession(), PRODUCT_GEN_PENDING,
                                  SessionNotificationState.OK, 1);
    }

    public void decrementProductGenerationPending(IngestRequest request) {
        sessionNotificationClient.decrement(request.getSessionOwner(), request.getSession(), PRODUCT_GEN_PENDING,
                                  SessionNotificationState.OK, 1);
    }

    public void incrementProductGenerationError(IngestRequest request) {
        sessionNotificationClient.increment(request.getSessionOwner(), request.getSession(), PRODUCT_GEN_ERROR,
                                  SessionNotificationState.ERROR, 1);
    }

    public void decrementProductGenerationError(IngestRequest request) {
        sessionNotificationClient.decrement(request.getSessionOwner(), request.getSession(), PRODUCT_GEN_ERROR,
                                  SessionNotificationState.ERROR, 1);
    }

    // File storage

    public void incrementProductStorePending(IngestRequest request) {
        sessionNotificationClient.increment(request.getSessionOwner(), request.getSession(), PRODUCT_STORE_PENDING,
                                  SessionNotificationState.OK, request.getAips().size());
        // Synchronize number of products according to available AIP(s)
        if (request.getAips().size() > 1) {
            // Increment number of total products (case of one SIP for many AIPs)
            // In this way, there is the same count of products stored and total.
            // 1 request => 1 product, but can produce several AIPs, so we add more product to match the number of AIPs
            incrementProductCount(request, request.getAips().size() - 1);
        }
    }

    public void decrementProductStorePending(IngestRequest request) {
        sessionNotificationClient.decrement(request.getSessionOwner(), request.getSession(), PRODUCT_STORE_PENDING,
                                  SessionNotificationState.OK, request.getAips().size());
    }

    public void decrementProductStore(IngestRequest request) {
        sessionNotificationClient.decrement(request.getSessionOwner(), request.getSession(), PRODUCT_STORED,
                                  SessionNotificationState.OK, request.getAips().size());
    }

    public void incrementProductStoreSuccess(IngestRequest request) {
        sessionNotificationClient.increment(request.getSessionOwner(), request.getSession(), PRODUCT_STORED,
                                  SessionNotificationState.OK, request.getAips().size());
    }

    public void incrementProductStoreError(IngestRequest request) {
        sessionNotificationClient.increment(request.getSessionOwner(), request.getSession(), PRODUCT_STORE_ERROR,
                                  SessionNotificationState.ERROR, request.getAips().size());
    }

    public void decrementProductStoreError(IngestRequest request) {
        sessionNotificationClient.decrement(request.getSessionOwner(), request.getSession(), PRODUCT_STORE_ERROR,
                                  SessionNotificationState.ERROR, request.getAips().size());
    }

    public void incrementProductIgnored(IngestRequest request) {
        if (request.getState() == InternalRequestState.IGNORED) {
            this.decrementProductGenerationPending(request);
            sessionNotificationClient.increment(request.getSessionOwner(), request.getSession(), PRODUCT_IGNORED,
                                      SessionNotificationState.OK, 1);
        }
    }

    public void incrementNewProductVersion(AIPEntity aipEntity) {
        sessionNotificationClient.increment(aipEntity.getSessionOwner(), aipEntity.getSession(), NEW_PRODUCT_VERSIONS,
                                  SessionNotificationState.OK, 1);
    }

    public void incrementProductReplace(AIPEntity aipEntity) {
        sessionNotificationClient.increment(aipEntity.getSessionOwner(), aipEntity.getSession(), PRODUCT_REPLACED,
                                  SessionNotificationState.OK, 1);
    }

    public void incrementProductWaitingVersioningMode(IngestRequest request) {
        if (request.getState() == InternalRequestState.WAITING_VERSIONING_MODE) {
            this.decrementProductGenerationPending(request);
            sessionNotificationClient.increment(request.getSessionOwner(), request.getSession(), PRODUCT_WAITING_VERSIONING_MODE,
                                      SessionNotificationState.OK, 1);
        }
    }

    public void decrementProductWaitingVersioningMode(IngestRequest request) {
        if (request.getState() == InternalRequestState.WAITING_VERSIONING_MODE) {
            sessionNotificationClient.decrement(request.getSessionOwner(), request.getSession(), PRODUCT_WAITING_VERSIONING_MODE,
                                      SessionNotificationState.OK, 1);
        }
    }

    // Post Process

    public void incrementPostProcessPending(AIPPostProcessRequest request) {
        sessionNotificationClient.increment(request.getSessionOwner(), request.getSession(), POST_PROCESS_PENDING,
                                  SessionNotificationState.OK, 1);
    }

    public void incrementPostProcessSuccess(AIPPostProcessRequest request) {
        sessionNotificationClient.increment(request.getSessionOwner(), request.getSession(), POST_PROCESS_SUCCESS,
                                  SessionNotificationState.OK, 1);
        sessionNotificationClient.decrement(request.getSessionOwner(), request.getSession(), POST_PROCESS_PENDING,
                                  SessionNotificationState.OK, 1);
    }

    public void incrementPostProcessError(AIPPostProcessRequest request) {
        sessionNotificationClient.increment(request.getSessionOwner(), request.getSession(), POST_PROCESS_ERROR,
                                  SessionNotificationState.ERROR, 1);
        sessionNotificationClient.decrement(request.getSessionOwner(), request.getSession(), POST_PROCESS_PENDING,
                                  SessionNotificationState.OK, 1);
    }

    public void decrementPostProcessError(AIPPostProcessRequest request) {
        sessionNotificationClient.decrement(request.getSessionOwner(), request.getSession(), POST_PROCESS_ERROR,
                                  SessionNotificationState.OK, 1);
    }

    public void decrementPostProcessPending(AIPPostProcessRequest request) {
        sessionNotificationClient.decrement(request.getSessionOwner(), request.getSession(), POST_PROCESS_PENDING,
                                  SessionNotificationState.OK, 1);
    }

    // AIP storage

    /**
     * Notify session when a request is deleted
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
     * @param sessionOwner
     * @param session
     * @param aips {@link AIPEntity}s of the deleted product
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
            sessionNotificationClient.decrement(sessionOwner, session, PRODUCT_STORE_PENDING, SessionNotificationState.OK,
                                      nbStorePending);
        }
        if (nbStored > 0) {
            // -x product_stored
            sessionNotificationClient.decrement(sessionOwner, session, PRODUCT_STORED, SessionNotificationState.OK, nbStored);
        }
        sessionNotificationClient.decrement(sessionOwner, session, PRODUCT_COUNT, SessionNotificationState.OK,
                                  nbGenerated + nbStored);
    }

}
