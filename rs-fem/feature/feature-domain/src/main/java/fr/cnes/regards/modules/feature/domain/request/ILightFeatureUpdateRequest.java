package fr.cnes.regards.modules.feature.domain.request;

import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.dto.urn.converter.FeatureUrnConverter;

import javax.persistence.Convert;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface ILightFeatureUpdateRequest extends IAbstractFeatureRequest {

    @Convert(converter = FeatureUrnConverter.class)
    FeatureUniformResourceName getUrn();

    String getProviderId();
}
