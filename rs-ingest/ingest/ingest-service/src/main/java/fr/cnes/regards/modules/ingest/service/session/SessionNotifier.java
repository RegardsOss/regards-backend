package fr.cnes.regards.modules.ingest.service.session;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionMonitoringEvent;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationOperator;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationState;
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
    private static final String PROPERTY_INGESTED = "total";

    /**
     * Name of the property that collects number of SIPs marked as deleted and waiting for storage deletion acquittal
     */
    private static final String PROPERTY_DELETING = "deleting";


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

    public void notifySIPDeleted(SIPEntity currentSIP) {
        if (currentSIP.getState() != SIPState.DELETED) {
            notifyDecrementSession(currentSIP.getIngestMetadata().getSessionOwner(),
                    currentSIP.getIngestMetadata().getSession(), currentSIP.getState());
        }
    }

    public void notifySIPCreated(SIPEntity currentSIP) {
        notifyIncrementSession(currentSIP.getIngestMetadata().getSessionOwner(),
                currentSIP.getIngestMetadata().getSession(), currentSIP.getState(), SessionNotificationState.OK);
    }

    public void notifySIPCreationFailed(SIPEntity sip) {
        notifyIncrementSession(sip.getIngestMetadata().getSessionOwner(),
                sip.getIngestMetadata().getSession(), sip.getState(), SessionNotificationState.ERROR);
    }

    private void notifyDecrementSession(String sessionOwner, String session, SIPState sipState) {
        SessionMonitoringEvent event = SessionMonitoringEvent.build(
                sessionOwner,
                session,
                SessionNotificationState.OK,
                SESSION_NOTIF_STEP,
                SessionNotificationOperator.DEC,
                getSIPProperty(sipState),
                1L
        );
        publisher.publish(event);
    }


    private void notifyIncrementSession(String sessionOwner, String session, SIPState sipState, SessionNotificationState notifState) {
        // Add one to the new state
        SessionMonitoringEvent event = SessionMonitoringEvent.build(
                sessionOwner,
                session,
                notifState,
                SESSION_NOTIF_STEP,
                SessionNotificationOperator.INC,
                getSIPProperty(sipState),
                1L
        );
        publisher.publish(event);
    }

    private String getSIPProperty(SIPState state) {
        switch (state) {
            case INGESTED:
                return PROPERTY_INGESTED;
        }
        return "";
    }

}
