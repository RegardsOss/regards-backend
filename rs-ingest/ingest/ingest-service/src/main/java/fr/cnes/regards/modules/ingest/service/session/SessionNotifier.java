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
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SessionNotifier {

    /**
     * The name of the property gathering all metadata about this processing step
     */
    private static final String SESSION_NOTIF_STEP = "OAIS";

    /**
     * Name of the property that collects number of SIPs generated and valid
     */
    private static final String PROPERTY_SIP_INGESTED = "sip_total";

    /**
     * Name of the property that collects number of SIPs marked as deleted and waiting for storage deletion acquittal
     */
    private static final String PROPERTY_SIP_DELETING = "sip_deleting";

    /**
     * Name of the property that collects number of SIPs marked as error
     */
    private static final String PROPERTY_SIP_ERROR = "sip_error";

    /**
     * Name of the property that collects number of AIPs generated and valid
     */
    private static final String PROPERTY_AIP_GENERATED = "aip_generated";

    /**
     * Name of the property that collects number of AIPs stored
     */
    private static final String PROPERTY_AIP_STORED = "aip_stored";

    /**
     * Name of the property that collects number of AIPs marked as deleted and waiting for storage deletion acquittal
     */
    private static final String PROPERTY_AIP_DELETING = "aip_deleting";

    /**
     * Name of the property that collects number of AIPs marked as error
     */
    private static final String PROPERTY_AIP_ERROR = "aip_error";


    @Autowired
    private IPublisher publisher;

    /**
     * Notify single SIP state change
     * @param metadata ingest metadata
     * @param previousState previous state
     * @param nextState next state
     */
    public void notifySIPStateChange(IngestMetadata metadata, SIPState previousState, SIPState nextState) {

        // Decrement the previous state by one
        notifyDecrementSession(metadata.getSessionOwner(), metadata.getSession(), previousState);

        // Increment the next state by one
        notifyIncrementSession(metadata.getSessionOwner(), metadata.getSession(), nextState, SessionNotificationState.OK);
    }

    public void notifySIPDeleting(SIPEntity currentSIP) {
        notifyDecrementSession(currentSIP.getIngestMetadata().getSessionOwner(),
                currentSIP.getIngestMetadata().getSession(), currentSIP.getState());
    }

    public void notifySIPCreated(SIPEntity currentSIP) {
        notifyIncrementSession(currentSIP.getIngestMetadata().getSessionOwner(),
                currentSIP.getIngestMetadata().getSession(), currentSIP.getState(), SessionNotificationState.OK);
    }

    public void notifySIPCreationFailed(SIPEntity sip) {
        notifyIncrementSession(sip.getIngestMetadata().getSessionOwner(),
                sip.getIngestMetadata().getSession(), sip.getState(), SessionNotificationState.ERROR);
    }

    public void notifyAIPDeleting(Set<AIPEntity> aipEntities) {
        if (!aipEntities.isEmpty()) {
            // Retrieve any AIP to retrieve session info
            AIPEntity anAIP = aipEntities.iterator().next();
            notifyDecrementSession(
                    anAIP.getIngestMetadata().getSessionOwner(), anAIP.getIngestMetadata().getSession(),
                    AIPState.STORED, aipEntities.size());

            notifyIncrementSession(
                    anAIP.getIngestMetadata().getSessionOwner(), anAIP.getIngestMetadata().getSession(),
                    anAIP.getState(), aipEntities.size());
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

    public void notifySIPDeleted(SIPEntity sipEntity) {
        notifyDecrementSession(sipEntity.getIngestMetadata().getSessionOwner(),
                sipEntity.getIngestMetadata().getSession(), sipEntity.getState());
    }

    private void notifyDecrementSession(String sessionOwner, String session, SIPState sipState) {
        notifyDecrementSession(sessionOwner, session, getSIPProperty(sipState), SessionNotificationState.OK, 1L);
    }

    private void notifyDecrementSession(String sessionOwner, String session, SIPState sipState, long value) {
        notifyDecrementSession(sessionOwner, session, getSIPProperty(sipState), SessionNotificationState.OK, value);
    }

    private void notifyDecrementSession(String sessionOwner, String session, AIPState aipState, long value) {
        notifyDecrementSession(sessionOwner, session, getAIPProperty(aipState), SessionNotificationState.OK, value);
    }
    private void notifyDecrementSession(String sessionOwner, String session, String property, SessionNotificationState notifState, long value) {
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


    private void notifyIncrementSession(String sessionOwner, String session, SIPState sipState, SessionNotificationState notifState) {
        // Add one to the new state
        notifyIncrementSession(sessionOwner, session, getSIPProperty(sipState), notifState, 1);
    }
    private void notifyIncrementSession(String sessionOwner, String session, AIPState aipState, long value) {
        // Add one to the new state
        notifyIncrementSession(sessionOwner, session, getAIPProperty(aipState), SessionNotificationState.OK, 1);
    }
    private void notifyIncrementSession(String sessionOwner, String session, AIPState aipState, SessionNotificationState notifState, long value) {
        // Add one to the new state
        notifyIncrementSession(sessionOwner, session, getAIPProperty(aipState), notifState, 1);
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
