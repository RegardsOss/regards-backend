package fr.cnes.regards.modules.processing.dto;

import fr.cnes.regards.modules.processing.domain.dto.PProcessDTO;
import fr.cnes.regards.modules.processing.testutils.AbstractMarshallingTest;

public class PProcessDTOTest extends AbstractMarshallingTest<PProcessDTO> {

    @Override public Class<PProcessDTO> testedType() {
        return PProcessDTO.class;
    }
}