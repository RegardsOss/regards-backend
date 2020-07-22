package fr.cnes.regards.modules.processing.dto;

import fr.cnes.regards.modules.processing.testutils.AbstractMarshallingTest;

public class PBatchRequestDTOTest extends AbstractMarshallingTest<PBatchRequest> {

    @Override public Class<PBatchRequest> testedType() {
        return PBatchRequest.class;
    }
}