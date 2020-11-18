package fr.cnes.regards.modules.order.service.job.parameters;

import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;

@lombok.Value
public class ProcessOutputFeatureDesc {
    String label;
    String ipId;

    public static ProcessOutputFeatureDesc from(EntityFeature feature) {
        return new ProcessOutputFeatureDesc(feature.getLabel(), feature.getId().toString());
    }
}
