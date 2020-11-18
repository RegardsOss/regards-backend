package fr.cnes.regards.modules.processing.domain;

import fr.cnes.regards.modules.processing.testutils.AbstractMarshallingTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PStepTest extends AbstractMarshallingTest<PStep> {

    @Override public Class<PStep> testedType() {
        return PStep.class;
    }

}