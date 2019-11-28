package fr.cnes.regards.modules.ingest.service.session;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionMonitoringEvent;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationOperator;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationState;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@MultitenantTransactional
public class SessionNotifier {

    public static final String SESSION_NOTIF_STEP = "oais";

    public static final String PRODUCT_COUNT = "products";

    public static final String PRODUCT_GEN_ERROR = "products_gen_error";

    public static final String PRODUCT_GEN_PENDING = "products_gen_pending";

    public static final String PRODUCT_STORE_PENDING = "products_store_pending";

    public static final String PRODUCT_STORED = "products_stored";

    public static final String PRODUCT_STORE_ERROR = "products_store_error";

    public static final String PRODUCT_META_STORE_PENDING = "products_meta_store_pending";

    public static final String PRODUCT_META_STORED = "products_meta_stored";

    public static final String PRODUCT_META_STORE_ERROR = "products_meta_store_error";

    @Autowired
    private IPublisher publisher;

    /**
     * Notify session that an error product ingest has been retried
     * @param sessionOwner
     * @param session
     */
    public void productRetry(String sessionOwner, String session) {
        // -1 product_generation_error
        notifyDecrementSession(sessionOwner, session, PRODUCT_GEN_ERROR, SessionNotificationState.OK, 1);
    }

    /**
     * Notify session that a list of products has been granted for generation
     * @param sessionOwner
     * @param session
     * @param nbProducts
     */
    public void productsGranted(String sessionOwner, String session, int nbProducts) {
        notifyIncrementSession(sessionOwner, session, PRODUCT_COUNT, SessionNotificationState.OK, nbProducts);
    }

    /**
     * Notify session that a product generation is started
     * @param sessionOwner
     * @param session
     */
    public void productGenerationStart(String sessionOwner, String session) {
        // +1 product_generation
        notifyIncrementSession(sessionOwner, session, PRODUCT_GEN_PENDING, SessionNotificationState.OK, 1);
    }

    /**
     * Notify a session that a product generation is ended. Handle success and error states.
     * @param sessionOwner
     * @param session
     * @param generatedAips {@link AIPEntity}s generated
     */
    public void productGenerationEnd(String sessionOwner, String session, Collection<AIPEntity> generatedAips) {
        // -1 product_generation
        notifyDecrementSession(sessionOwner, session, PRODUCT_GEN_PENDING, SessionNotificationState.OK, 1);
        if (generatedAips.isEmpty()) {
            // +1 product_generation_error
            notifyIncrementSession(sessionOwner, session, PRODUCT_GEN_ERROR, SessionNotificationState.ERROR, 1);
        } else {
            notifyIncrementSession(sessionOwner, session, PRODUCT_STORE_PENDING, SessionNotificationState.OK,
                    generatedAips.size());
            if (generatedAips.size() > 1) {
                // Increment number of total products (case of one SIP for many AIPs)
                // In this way, there is the same count of products stored and total.
                // 1 request => 1 product, but can produce several AIPs, so we add more product to match the number of AIPs
                notifyIncrementSession(sessionOwner, session, PRODUCT_COUNT, SessionNotificationState.OK,
                                       generatedAips.size() - 1);
            }
        }
    }

    /**
     * Notify session that a product has been successfully stored
     * @param sessionOwner
     * @param session
     * @param aips {@link AIPEntity}s stored
     */
    public void productStoreSuccess(String sessionOwner, String session, Collection<AIPEntity> aips) {
        if (!aips.isEmpty()) {
            // -nbProductsStored product_storing
            notifyDecrementSession(sessionOwner, session, PRODUCT_STORE_PENDING, SessionNotificationState.OK,
                                   aips.size());
            // +nbProductsStored product_stored
            notifyIncrementSession(sessionOwner, session, PRODUCT_STORED, SessionNotificationState.OK, aips.size());
        }
    }

    /**
     * Notify session that there was an error during product storage
     * @param sessionOwner
     * @param session
     * @param aips {@link AIPEntity}s of the error product
     */
    public void productStoreError(String sessionOwner, String session, Collection<AIPEntity> aips) {
        if (!aips.isEmpty()) {
            // -nbProductsStored product_storing
            notifyDecrementSession(sessionOwner, session, PRODUCT_STORE_PENDING, SessionNotificationState.OK,
                                   aips.size());
            // +nbProductsStored product_stored
            notifyIncrementSession(sessionOwner, session, PRODUCT_STORE_ERROR, SessionNotificationState.ERROR,
                                   aips.size());
        }
    }

    /**
     * Notify session that a product metadata storage is pending
     */
    public void productMetaStorePending(String sessionOwner, String session, Collection<AIPEntity> aips) {
        if (!aips.isEmpty()) {
            // +1 product_meta_store_pending
            notifyIncrementSession(sessionOwner, session, PRODUCT_META_STORE_PENDING, SessionNotificationState.OK,
                                   aips.size());
        }
    }

    /**
     * Notify session that a product metadata storage is succressfully done
     * @param aip {@link AIPEntity}
     */
    public void productMetaStoredSuccess(AIPEntity aip) {
        // -1 product_meta_storing
        notifyDecrementSession(aip.getSessionOwner(), aip.getSession(),
                               PRODUCT_META_STORE_PENDING, SessionNotificationState.OK, 1);
        // +1 product_meta_stored
        notifyIncrementSession(aip.getSessionOwner(), aip.getSession(),
                               PRODUCT_META_STORED, SessionNotificationState.OK, 1);
    }

    /**
     * Notify session that a product metadata storage is in error
     * @param aip {@link AIPEntity}
     */
    public void productMetaStoredError(AIPEntity aip) {
        // -1 product_meta_storing
        notifyDecrementSession(aip.getSessionOwner(), aip.getSession(),
                               PRODUCT_META_STORE_PENDING, SessionNotificationState.OK, 1);
        // +1 product_meta_stored
        notifyIncrementSession(aip.getSessionOwner(), aip.getSession(),
                               PRODUCT_META_STORE_ERROR, SessionNotificationState.ERROR, 1);
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
        for (AIPEntity aip : aips) {
            switch (aip.getState()) {
                case GENERATED:
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
        if (nbGenerated > 0) {
            // -x product_storing
            notifyDecrementSession(sessionOwner, session, PRODUCT_STORE_PENDING, SessionNotificationState.OK,
                                   nbGenerated);
        }
        if (nbStored > 0) {
            // -x product_stored
            notifyDecrementSession(sessionOwner, session, PRODUCT_STORED, SessionNotificationState.OK, nbStored);
            // TODO -x product_meta_stored ???
        }
        notifyDecrementSession(sessionOwner, session, PRODUCT_COUNT, SessionNotificationState.OK,
                               nbGenerated + nbStored);
    }

    private void notifyIncrementSession(String sessionOwner, String session, String property,
            SessionNotificationState notifState, long value) {
        // Add one to the new state
        SessionMonitoringEvent event = SessionMonitoringEvent.build(sessionOwner, session, notifState,
                                                                    SESSION_NOTIF_STEP, SessionNotificationOperator.INC,
                                                                    property, value);
        publisher.publish(event);
    }

    private void notifyDecrementSession(String sessionOwner, String session, String property,
            SessionNotificationState notifState, long value) {
        // Add one to the new state
        SessionMonitoringEvent event = SessionMonitoringEvent.build(sessionOwner, session, notifState,
                                                                    SESSION_NOTIF_STEP, SessionNotificationOperator.DEC,
                                                                    property, value);
        publisher.publish(event);
    }
}
