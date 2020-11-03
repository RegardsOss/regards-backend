package fr.cnes.regards.modules.order.service.job.parameters;

import fr.cnes.regards.modules.order.domain.OrderDataFile;

import java.util.List;
import java.util.Map;

@lombok.Value
public class ProcessInputsPerFeature {
    Map<ProcessOutputFeatureDesc, List<OrderDataFile>> filesPerFeature;
}
