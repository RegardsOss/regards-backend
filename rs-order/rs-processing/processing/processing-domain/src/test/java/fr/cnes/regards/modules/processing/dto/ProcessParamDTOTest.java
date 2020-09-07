package fr.cnes.regards.modules.processing.dto;

import fr.cnes.regards.modules.processing.domain.dto.ExecutionParamDTO;
import fr.cnes.regards.modules.processing.testutils.AbstractMarshallingTest;

public class ProcessParamDTOTest extends AbstractMarshallingTest<ExecutionParamDTO> {

    @Override public Class<ExecutionParamDTO> testedType() {
        return ExecutionParamDTO.class;
    }
}