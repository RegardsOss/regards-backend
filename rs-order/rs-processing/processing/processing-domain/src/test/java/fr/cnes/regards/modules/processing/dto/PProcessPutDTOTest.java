package fr.cnes.regards.modules.processing.dto;

import fr.cnes.regards.modules.processing.domain.dto.PProcessPutDTO;
import fr.cnes.regards.modules.processing.testutils.AbstractMarshallingTest;

public class PProcessPutDTOTest extends AbstractMarshallingTest<PProcessPutDTO> {

    @Override public Class<PProcessPutDTO> testedType() {
        return PProcessPutDTO.class;
    }
}