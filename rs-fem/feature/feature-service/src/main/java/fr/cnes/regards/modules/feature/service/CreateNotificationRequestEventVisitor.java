package fr.cnes.regards.modules.feature.service;

import com.google.gson.Gson;
import fr.cnes.regards.framework.amqp.event.notifier.NotificationRequestEvent;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityRepository;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.*;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureManagementAction;

import java.util.Optional;
import java.util.Set;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class CreateNotificationRequestEventVisitor
    implements IAbstractFeatureRequestVisitor<Optional<NotificationRequestEvent>> {

    public static class NotificationActionEventMetadata {

        private String action;

        private final String sessionOwner;

        private final String session;

        public NotificationActionEventMetadata(FeatureManagementAction action, String sessionOwner, String session) {
            this.action = action.toString();
            this.sessionOwner = sessionOwner;
            this.session = session;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getSessionOwner() {
            return sessionOwner;
        }

        public String getSession() {
            return session;
        }
    }

    private final Gson gson;

    private final IFeatureEntityRepository featureRepo;

    private final Set<AbstractFeatureRequest> visitorErrorRequests;

    public CreateNotificationRequestEventVisitor(Gson gson,
                                                 IFeatureEntityRepository featureRepo,
                                                 Set<AbstractFeatureRequest> visitorErrorRequests) {
        this.gson = gson;
        this.featureRepo = featureRepo;
        this.visitorErrorRequests = visitorErrorRequests;
    }

    @Override
    public Optional<NotificationRequestEvent> visitCreationRequest(FeatureCreationRequest creationRequest) {
        return Optional.of(new NotificationRequestEvent(gson.toJsonTree(creationRequest.getFeatureEntity().getFeature())
                                                            .getAsJsonObject(),
                                                        gson.toJsonTree(new NotificationActionEventMetadata(
                                                                FeatureManagementAction.CREATED,
                                                                creationRequest.getFeatureEntity().getSessionOwner(),
                                                                creationRequest.getFeatureEntity().getSession()))
                                                            .getAsJsonObject(),
                                                        creationRequest.getRequestId(),
                                                        creationRequest.getRequestOwner()));
    }

    @Override
    public Optional<NotificationRequestEvent> visitDeletionRequest(FeatureDeletionRequest deletionRequest) {
        FeatureManagementAction action = deletionRequest.isAlreadyDeleted() ? FeatureManagementAction.ALREADY_DELETED :
            FeatureManagementAction.DELETED;
        NotificationActionEventMetadata metadata = new NotificationActionEventMetadata(action,
                                                                                       deletionRequest.getSourceToNotify(),
                                                                                       deletionRequest.getSessionToNotify());


        return Optional.of(new NotificationRequestEvent(gson.toJsonTree(deletionRequest.getToNotify())
                                                            .getAsJsonObject(),
                                                        gson.toJsonTree(metadata).getAsJsonObject(),
                                                        deletionRequest.getRequestId(),
                                                        deletionRequest.getRequestOwner()));
    }

    @Override
    public Optional<NotificationRequestEvent> visitCopyRequest(FeatureCopyRequest copyRequest) {
        // this type of request might not be notified but in case it is but i've not seen it lets use basic logic.
        // if perfs are crappy inspire yourself from what has been done for update requests or feature notification requests
        FeatureEntity featureEntity = featureRepo.findByUrn(copyRequest.getUrn());
        if (featureEntity != null) {
            return Optional.of(new NotificationRequestEvent(gson.toJsonTree(featureEntity.getFeature())
                                                                .getAsJsonObject(),
                                                            gson.toJsonTree(new NotificationActionEventMetadata(
                                                                FeatureManagementAction.COPY,
                                                                featureEntity.getSessionOwner(),
                                                                featureEntity.getSession())).getAsJsonObject(),
                                                            copyRequest.getRequestId(),
                                                            copyRequest.getRequestOwner()));
        } else {
            visitorErrorRequests.add(copyRequest);
            return Optional.empty();
        }
    }

    @Override
    public Optional<NotificationRequestEvent> visitUpdateRequest(FeatureUpdateRequest updateRequest) {
        Feature feature = updateRequest.getToNotify();
        if (feature != null) {
            return Optional.of(new NotificationRequestEvent(gson.toJsonTree(feature).getAsJsonObject(),
                                                            gson.toJsonTree(new NotificationActionEventMetadata(
                                                                FeatureManagementAction.UPDATED,
                                                                updateRequest.getSourceToNotify(),
                                                                updateRequest.getSessionToNotify())).getAsJsonObject(),
                                                            updateRequest.getRequestId(),
                                                            updateRequest.getRequestOwner()));
        } else {
            visitorErrorRequests.add(updateRequest);
            return Optional.empty();
        }
    }

    @Override
    public Optional<NotificationRequestEvent> visitNotificationRequest(FeatureNotificationRequest featureNotificationRequest) {
        Feature feature = featureNotificationRequest.getToNotify();
        if (feature != null) {
            if (!featureNotificationRequest.getRecipientIds().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new NotificationRequestEvent(gson.toJsonTree(feature).getAsJsonObject(),
                                                            gson.toJsonTree(new NotificationActionEventMetadata(
                                                                    FeatureManagementAction.NOTIFIED,
                                                                    featureNotificationRequest.getSourceToNotify(),
                                                                    featureNotificationRequest.getSessionToNotify()))
                                                                .getAsJsonObject(),
                                                            featureNotificationRequest.getRequestId(),
                                                            featureNotificationRequest.getRequestOwner()));

        } else {
            visitorErrorRequests.add(featureNotificationRequest);
            return Optional.empty();
        }
    }
}
