package fr.cnes.regards.modules.ingest.service.session;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionMonitoringEvent;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationOperator;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationState;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SessionNotifier {

    /**
     * The name of the property gathering all metadata about this processing step
     */
    public static final String SESSION_NOTIF_STEP = "OAIS";

    /**
     * Name of the property that collects number of SIPs generated
     */
    public static final String PROPERTY_SIP_INGESTING = "sip_ingesting";

    /**
     * Name of the property that collects number of SIPs valid and stored
     */
    public static final String PROPERTY_SIP_INGESTED = "sip_total";

    /**
     * Name of the property that collects number of SIPs marked as deleted and waiting for storage deletion acquittal
     */
    public static final String PROPERTY_SIP_DELETING = "sip_deleting";

    /**
     * Name of the property that collects number of SIPs marked as error
     */
    public static final String PROPERTY_SIP_ERROR = "sip_error";

    /**
     * Name of the property that collects number of AIPs generated and valid
     */
    public static final String PROPERTY_AIP_GENERATED = "aip_generated";

    /**
     * Name of the property that collects number of AIPs stored
     */
    public static final String PROPERTY_AIP_STORED = "aip_stored";

    /**
     * Name of the property that collects number of AIPs marked as deleted and waiting for storage deletion acquittal
     */
    public static final String PROPERTY_AIP_DELETING = "aip_deleting";

    /**
     * Name of the property that collects number of AIPs marked as error
     */
    public static final String PROPERTY_AIP_ERROR = "aip_error";


    @Autowired
    private IPublisher publisher;

    public void notifySIPDeleting(SIPEntity currentSIP) {
        notifyDecrementSession(currentSIP.getIngestMetadata().getSessionOwner(),
                currentSIP.getIngestMetadata().getSession(), currentSIP.getState());

        notifyIncrementSession(currentSIP.getIngestMetadata().getSessionOwner(),
                currentSIP.getIngestMetadata().getSession(), SIPState.DELETED);
    }

    public void notifySIPDeleted(SIPEntity sipEntity) {
        notifyDecrementSession(sipEntity.getIngestMetadata().getSessionOwner(),
                sipEntity.getIngestMetadata().getSession(), sipEntity.getState());
    }

    public void notifySIPStored(SIPEntity currentSIP) {
        notifyDecrementSession(currentSIP.getIngestMetadata().getSessionOwner(),
                currentSIP.getIngestMetadata().getSession(), SIPState.INGESTED);

        notifyIncrementSession(currentSIP.getIngestMetadata().getSessionOwner(),
                currentSIP.getIngestMetadata().getSession(), currentSIP.getState());
    }
    public void notifySIPStorageFailed(SIPEntity sip) {
        notifyDecrementSession(sip.getIngestMetadata().getSessionOwner(),
                sip.getIngestMetadata().getSession(), sip.getState());

        notifyIncrementSession(sip.getIngestMetadata().getSessionOwner(),
                sip.getIngestMetadata().getSession(), SIPState.ERROR, SessionNotificationState.ERROR);
    }

    public void notifySIPCreated(SIPEntity sipEntity) {
        notifyIncrementSession(sipEntity.getIngestMetadata().getSessionOwner(),
                sipEntity.getIngestMetadata().getSession(), sipEntity.getState());
    }

    public void notifySIPCreationFailed(SIPEntity sip) {
        notifyIncrementSession(sip.getIngestMetadata().getSessionOwner(),
                sip.getIngestMetadata().getSession(), SIPState.ERROR, SessionNotificationState.ERROR);
    }

    public void notifySIPDeletionFailed(SIPEntity currentSIP) {
        notifyDecrementSession(currentSIP.getIngestMetadata().getSessionOwner(),
                currentSIP.getIngestMetadata().getSession(), SIPState.DELETED);

        notifyIncrementSession(currentSIP.getIngestMetadata().getSessionOwner(),
                currentSIP.getIngestMetadata().getSession(), SIPState.ERROR, SessionNotificationState.ERROR);
    }

    public void notifyAIPCreated(List<AIPEntity> aipEntities) {
        if (!aipEntities.isEmpty()) {
            // Retrieve any AIP to retrieve session info
            AIPEntity anAIP = aipEntities.iterator().next();
            notifyIncrementSession(
                    anAIP.getIngestMetadata().getSessionOwner(), anAIP.getIngestMetadata().getSession(),
                    anAIP.getState(), aipEntities.size());
        }
    }

    public void notifyAIPStorageFailed(AIPEntity aipEntity) {
        if (aipEntity.getState() != AIPState.ERROR) {
            notifyDecrementSession(
                    aipEntity.getIngestMetadata().getSessionOwner(), aipEntity.getIngestMetadata().getSession(),
                    aipEntity.getState(), 1);
            notifyIncrementSession(
                    aipEntity.getIngestMetadata().getSessionOwner(), aipEntity.getIngestMetadata().getSession(),
                    AIPState.ERROR, SessionNotificationState.ERROR, 1);
        }
    }

    public void notifyAIPsStored(List<AIPEntity> aipEntities) {
        if (!aipEntities.isEmpty()) {
            // Retrieve any AIP to retrieve session info
            AIPEntity anAIP = aipEntities.iterator().next();
            notifyDecrementSession(
                    anAIP.getIngestMetadata().getSessionOwner(), anAIP.getIngestMetadata().getSession(),
                    AIPState.GENERATED, aipEntities.size());
            notifyIncrementSession(
                    anAIP.getIngestMetadata().getSessionOwner(), anAIP.getIngestMetadata().getSession(),
                    anAIP.getState(), aipEntities.size());
        }
    }

    public void notifyAIPDeleting(Set<AIPEntity> aipEntities) {
        if (!aipEntities.isEmpty()) {
            // Retrieve any AIP to retrieve session info
            AIPEntity anAIP = aipEntities.iterator().next();
            notifyDecrementSession(
                    anAIP.getIngestMetadata().getSessionOwner(), anAIP.getIngestMetadata().getSession(),
                    anAIP.getState(), aipEntities.size());

            notifyIncrementSession(
                    anAIP.getIngestMetadata().getSessionOwner(), anAIP.getIngestMetadata().getSession(),
                    AIPState.DELETED, aipEntities.size());
        }
    }

    public void notifyAIPDeleted(Set<AIPEntity> aipEntities) {
        if (!aipEntities.isEmpty()) {
            // Retrieve any AIP to retrieve session info
            AIPEntity anAIP = aipEntities.iterator().next();
            // AIPs are no more deleting, remove them
            notifyDecrementSession(
                    anAIP.getIngestMetadata().getSessionOwner(), anAIP.getIngestMetadata().getSession(),
                    AIPState.DELETED, aipEntities.size());
        }
    }

    public void notifyAIPDeletionFailed(Set<AIPEntity> aipEntities) {
        if (!aipEntities.isEmpty()) {
            // Retrieve any AIP to retrieve session info
            AIPEntity anAIP = aipEntities.iterator().next();
            // AIPs are no more deleting, remove them
            notifyDecrementSession(
                    anAIP.getIngestMetadata().getSessionOwner(), anAIP.getIngestMetadata().getSession(),
                    AIPState.DELETED, aipEntities.size());
            // Add errors
            notifyIncrementSession(
                    anAIP.getIngestMetadata().getSessionOwner(), anAIP.getIngestMetadata().getSession(),
                    AIPState.ERROR, SessionNotificationState.ERROR, aipEntities.size());
        }
    }

    private void notifyDecrementSession(String sessionOwner, String session, SIPState sipState) {
        notifyDecrementSession(sessionOwner, session, getSIPProperty(sipState), SessionNotificationState.OK, 1L);
    }

    private void notifyDecrementSession(String sessionOwner, String session, AIPState aipState, long value) {
        notifyDecrementSession(sessionOwner, session, getAIPProperty(aipState), SessionNotificationState.OK, value);
    }
    private void notifyDecrementSession(String sessionOwner, String session, String property,
            SessionNotificationState notifState, long value) {
        // Add one to the new state
        SessionMonitoringEvent event = SessionMonitoringEvent.build(
                sessionOwner,
                session,
                notifState,
                SESSION_NOTIF_STEP,
                SessionNotificationOperator.DEC,
                property,
                value
        );
        publisher.publish(event);
    }


    private void notifyIncrementSession(String sessionOwner, String session, SIPState sipState) {
        // Add one to the new state
        notifyIncrementSession(sessionOwner, session, getSIPProperty(sipState), SessionNotificationState.OK, 1);
    }
    private void notifyIncrementSession(String sessionOwner, String session, SIPState sipState, SessionNotificationState notifState) {
        // Add one to the new state
        notifyIncrementSession(sessionOwner, session, getSIPProperty(sipState), notifState, 1);
    }
    private void notifyIncrementSession(String sessionOwner, String session, AIPState aipState, long value) {
        // Add one to the new state
        notifyIncrementSession(sessionOwner, session, getAIPProperty(aipState), SessionNotificationState.OK, value);
    }
    private void notifyIncrementSession(String sessionOwner, String session, AIPState aipState, SessionNotificationState notifState, long value) {
        // Add one to the new state
        notifyIncrementSession(sessionOwner, session, getAIPProperty(aipState), notifState, value);
    }
    private void notifyIncrementSession(String sessionOwner, String session, String property, SessionNotificationState notifState, long value) {
        // Add one to the new state
        SessionMonitoringEvent event = SessionMonitoringEvent.build(
                sessionOwner,
                session,
                notifState,
                SESSION_NOTIF_STEP,
                SessionNotificationOperator.INC,
                property,
                value
        );
        publisher.publish(event);
    }

    private String getSIPProperty(SIPState state) {
        switch (state) {
            case INGESTED:
                return PROPERTY_SIP_INGESTING;
            case STORED:
                return PROPERTY_SIP_INGESTED;
            case DELETED:
                return PROPERTY_SIP_DELETING;
            case ERROR:
                return PROPERTY_SIP_ERROR;
        }
        return "";
    }
    private String getAIPProperty(AIPState state) {
        switch (state) {
            case GENERATED:
                return PROPERTY_AIP_GENERATED;
            case STORED:
                return PROPERTY_AIP_STORED;
            case DELETED:
                return PROPERTY_AIP_DELETING;
            case ERROR:
                return PROPERTY_AIP_ERROR;
        }
        return "";
    }

}
