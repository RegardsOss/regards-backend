package fr.cnes.regards.modules.feature.domain;

import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.dto.urn.converter.FeatureUrnConverter;

import javax.persistence.Convert;

public interface ILightFeatureEntity {

    @Convert(converter = FeatureUrnConverter.class)
    FeatureUniformResourceName getUrn();

    String getSessionOwner();

    String getSession();

}
