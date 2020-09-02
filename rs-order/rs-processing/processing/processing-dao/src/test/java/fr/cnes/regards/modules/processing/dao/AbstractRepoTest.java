package fr.cnes.regards.modules.processing.dao;

import fr.cnes.regards.modules.processing.repository.IPBatchRepository;
import fr.cnes.regards.modules.processing.repository.IPExecutionRepository;
import fr.cnes.regards.modules.processing.testutils.AbstractProcessingTest;
import fr.cnes.regards.modules.processing.testutils.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractRepoTest extends AbstractProcessingTest implements RandomUtils {

    @Autowired protected IPBatchRepository domainBatchRepo;
    @Autowired protected IPExecutionRepository domainExecRepo;
    @Autowired protected IBatchEntityRepository entityBatchRepo;
    @Autowired protected IExecutionEntityRepository entityExecRepo;

}
