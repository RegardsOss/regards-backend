package fr.cnes.regards.modules.processing.dto;

import fr.cnes.regards.modules.processing.testutils.AbstractMarshallingTest;

import static org.junit.Assert.*;

public class PProcessDTOTest extends AbstractMarshallingTest<PProcessDTO> {

    @Override public Class<PProcessDTO> testedType() {
        return PProcessDTO.class;
    }
}