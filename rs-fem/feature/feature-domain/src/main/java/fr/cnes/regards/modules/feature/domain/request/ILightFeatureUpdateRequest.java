package fr.cnes.regards.modules.feature.domain.request;

import javax.persistence.Convert;

import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.dto.urn.converter.FeatureUrnConverter;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface ILightFeatureUpdateRequest extends IAbstractFeatureRequest {

    @Convert(converter = FeatureUrnConverter.class)
    FeatureUniformResourceName getUrn();

    String getProviderId();
}
