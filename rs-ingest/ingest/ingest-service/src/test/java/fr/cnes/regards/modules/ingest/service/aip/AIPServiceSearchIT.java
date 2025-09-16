package fr.cnes.regards.modules.ingest.service.aip;

import fr.cnes.regards.modules.ingest.dto.AIPEntityLightRawDto;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

public class AIPServiceSearchIT extends AbstractAIPServiceSearchIT {

    @Override
    protected Page<AIPEntityLightRawDto> findByFilters(SearchAIPsParameters searchAIPsParameters) {
        return aipService.findLightByFilters(searchAIPsParameters, PageRequest.of(0, 100));
    }
}
