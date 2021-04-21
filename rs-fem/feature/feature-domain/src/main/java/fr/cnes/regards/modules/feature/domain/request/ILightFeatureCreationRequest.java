package fr.cnes.regards.modules.feature.domain.request;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface ILightFeatureCreationRequest extends IAbstractFeatureRequest {

    FeatureCreationMetadataEntity getMetadata();

    String getProviderId();
}
