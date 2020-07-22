package fr.cnes.regards.modules.processing.dto;

import fr.cnes.regards.modules.processing.testutils.AbstractMarshallingTest;

public class PBatchResponseDTOTest extends AbstractMarshallingTest<PBatchResponse> {

    @Override public Class<PBatchResponse> testedType() {
        return PBatchResponse.class;
    }
}