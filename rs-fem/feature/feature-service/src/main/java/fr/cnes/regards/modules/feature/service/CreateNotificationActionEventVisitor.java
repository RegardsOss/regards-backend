package fr.cnes.regards.modules.feature.service;

import com.google.gson.Gson;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityRepository;
import fr.cnes.regards.modules.feature.domain.request.FeatureCopyRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureDeletionRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureUpdateRequest;
import fr.cnes.regards.modules.feature.domain.request.IAbstractFeatureRequestVisitor;
import fr.cnes.regards.modules.feature.domain.request.NotificationRequest;
import fr.cnes.regards.modules.feature.dto.FeatureManagementAction;
import fr.cnes.regards.modules.notifier.dto.in.NotificationActionEvent;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class CreateNotificationActionEventVisitor implements IAbstractFeatureRequestVisitor<NotificationActionEvent> {

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

    private Gson gson;

    private IFeatureEntityRepository featureRepo;

    public CreateNotificationActionEventVisitor(Gson gson, IFeatureEntityRepository featureRepo) {
        this.gson = gson;
        this.featureRepo = featureRepo;
    }

    @Override
    public NotificationActionEvent visitCreationRequest(FeatureCreationRequest creationRequest) {
        return new NotificationActionEvent(gson.toJsonTree(creationRequest.getFeature()),
                                           gson.toJsonTree(new NotificationActionEventMetadata(FeatureManagementAction.CREATED)),
                                           creationRequest.getRequestId(),
                                           creationRequest.getRequestOwner());
    }

    @Override
    public NotificationActionEvent visitDeletionRequest(FeatureDeletionRequest deletionRequest) {
        if (deletionRequest.isAlreadyDeleted()) {
            return new NotificationActionEvent(gson.toJsonTree(deletionRequest.getToNotify()),
                                               gson.toJsonTree(new NotificationActionEventMetadata(
                                                       FeatureManagementAction.ALREADY_DELETED)),
                                               deletionRequest.getRequestId(),
                                               deletionRequest.getRequestOwner());
        } else {
            return new NotificationActionEvent(gson.toJsonTree(deletionRequest.getToNotify()),
                                               gson.toJsonTree(new NotificationActionEventMetadata(
                                                       FeatureManagementAction.DELETED)),
                                               deletionRequest.getRequestId(),
                                               deletionRequest.getRequestOwner());
        }
    }

    @Override
    public NotificationActionEvent visitCopyRequest(FeatureCopyRequest copyRequest) {
        return new NotificationActionEvent(gson.toJsonTree(featureRepo.findByUrn(copyRequest.getUrn()).getFeature()),
                                           gson.toJsonTree(new NotificationActionEventMetadata(FeatureManagementAction.COPY)),
                                           copyRequest.getRequestId(),
                                           copyRequest.getRequestOwner());
    }

    @Override
    public NotificationActionEvent visitUpdateRequest(FeatureUpdateRequest updateRequest) {
        return new NotificationActionEvent(gson.toJsonTree(featureRepo.findByUrn(updateRequest.getUrn()).getFeature()),
                                           gson.toJsonTree(new NotificationActionEventMetadata(FeatureManagementAction.UPDATED)),
                                           updateRequest.getRequestId(),
                                           updateRequest.getRequestOwner());
    }

    @Override
    public NotificationActionEvent visitNotificationRequest(NotificationRequest notificationRequest) {
        return new NotificationActionEvent(gson.toJsonTree(featureRepo.findByUrn(notificationRequest.getUrn())
                                                                   .getFeature()),
                                           gson.toJsonTree(new NotificationActionEventMetadata(FeatureManagementAction.NOTIFIED)),
                                           notificationRequest.getRequestId(),
                                           notificationRequest.getRequestOwner());
    }
}
