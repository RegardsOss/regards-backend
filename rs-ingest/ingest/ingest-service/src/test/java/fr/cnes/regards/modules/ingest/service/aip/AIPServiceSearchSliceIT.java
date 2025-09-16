package fr.cnes.regards.modules.ingest.service.aip;

import fr.cnes.regards.modules.ingest.dto.AIPEntityLightRawDto;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public class AIPServiceSearchSliceIT extends AbstractAIPServiceSearchIT {

    @Override
    protected Slice<AIPEntityLightRawDto> findByFilters(SearchAIPsParameters searchAIPsParameters) {
        return aipService.findLightSliceByFilters(searchAIPsParameters, Pageable.ofSize(100));
    }
}
