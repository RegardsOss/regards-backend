package fr.cnes.regards.modules.feature.domain.request;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IAbstractFeatureRequestVisitor<U> {

    U visitCreationRequest(FeatureCreationRequest creationRequest);

    U visitDeletionRequest(FeatureDeletionRequest deletionRequest);

    U visitCopyRequest(FeatureCopyRequest copyRequest);

    U visitUpdateRequest(FeatureUpdateRequest updateRequest);

    U visitNotificationRequest(FeatureNotificationRequest featureNotificationRequest);
}
