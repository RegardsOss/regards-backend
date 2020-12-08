package fr.cnes.regards.modules.feature.service;

import com.google.gson.Gson;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityRepository;
import fr.cnes.regards.modules.feature.domain.request.FeatureCopyRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureDeletionRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureUpdateRequest;
import fr.cnes.regards.modules.feature.domain.request.IAbstractFeatureRequestVisitor;
import fr.cnes.regards.modules.feature.domain.request.FeatureNotificationRequest;
import fr.cnes.regards.modules.feature.dto.FeatureManagementAction;
import fr.cnes.regards.modules.notifier.dto.in.NotificationRequestEvent;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class CreateNotificationRequestEventVisitor implements IAbstractFeatureRequestVisitor<NotificationRequestEvent> {

    public static class NotificationActionEventMetadata {

        private String action;

        public NotificationActionEventMetadata(FeatureManagementAction action) {
            this.action = action.toString();
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }
    }

    private final Gson gson;

    private final IFeatureEntityRepository featureRepo;

    public CreateNotificationRequestEventVisitor(Gson gson, IFeatureEntityRepository featureRepo) {
        this.gson = gson;
        this.featureRepo = featureRepo;
    }

    @Override
    public NotificationRequestEvent visitCreationRequest(FeatureCreationRequest creationRequest) {
        return new NotificationRequestEvent(gson.toJsonTree(creationRequest.getFeature()).getAsJsonObject(),
                                            gson.toJsonTree(new NotificationActionEventMetadata(FeatureManagementAction.CREATED)),
                                            creationRequest.getRequestId(),
                                            creationRequest.getRequestOwner());
    }

    @Override
    public NotificationRequestEvent visitDeletionRequest(FeatureDeletionRequest deletionRequest) {
        if (deletionRequest.isAlreadyDeleted()) {
            return new NotificationRequestEvent(gson.toJsonTree(deletionRequest.getToNotify()).getAsJsonObject(),
                                                gson.toJsonTree(new NotificationActionEventMetadata(
                                                       FeatureManagementAction.ALREADY_DELETED)),
                                                deletionRequest.getRequestId(),
                                                deletionRequest.getRequestOwner());
        } else {
            return new NotificationRequestEvent(gson.toJsonTree(deletionRequest.getToNotify()).getAsJsonObject(),
                                                gson.toJsonTree(new NotificationActionEventMetadata(
                                                       FeatureManagementAction.DELETED)),
                                                deletionRequest.getRequestId(),
                                                deletionRequest.getRequestOwner());
        }
    }

    @Override
    public NotificationRequestEvent visitCopyRequest(FeatureCopyRequest copyRequest) {
        return new NotificationRequestEvent(gson.toJsonTree(featureRepo.findByUrn(copyRequest.getUrn()).getFeature()).getAsJsonObject(),
                                            gson.toJsonTree(new NotificationActionEventMetadata(FeatureManagementAction.COPY)),
                                            copyRequest.getRequestId(),
                                            copyRequest.getRequestOwner());
    }

    @Override
    public NotificationRequestEvent visitUpdateRequest(FeatureUpdateRequest updateRequest) {
        return new NotificationRequestEvent(gson.toJsonTree(featureRepo.findByUrn(updateRequest.getUrn()).getFeature()).getAsJsonObject(),
                                            gson.toJsonTree(new NotificationActionEventMetadata(FeatureManagementAction.UPDATED)),
                                            updateRequest.getRequestId(),
                                            updateRequest.getRequestOwner());
    }

    @Override
    public NotificationRequestEvent visitNotificationRequest(FeatureNotificationRequest featureNotificationRequest) {
        return new NotificationRequestEvent(gson.toJsonTree(featureRepo.findByUrn(featureNotificationRequest.getUrn())
                                                                   .getFeature()).getAsJsonObject(),
                                            gson.toJsonTree(new NotificationActionEventMetadata(FeatureManagementAction.NOTIFIED)),
                                            featureNotificationRequest.getRequestId(),
                                            featureNotificationRequest.getRequestOwner());
    }
}
