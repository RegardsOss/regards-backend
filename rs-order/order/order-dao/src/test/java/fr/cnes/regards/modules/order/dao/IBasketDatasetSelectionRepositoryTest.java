package fr.cnes.regards.modules.order.dao;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalTest;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.process.ProcessDatasetDescription;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(locations = { "classpath:test.properties" })
public class IBasketDatasetSelectionRepositoryTest extends AbstractDaoTransactionalTest {

    @Autowired private IBasketDatasetSelectionRepository dsSelRepo;

    @Test public void testFindByProcessId () {
        UUID processBusinessId = UUID.randomUUID();

        BasketDatasetSelection dsSelWithoutProcess = new BasketDatasetSelection();
        dsSelWithoutProcess.setDatasetIpid("some ip id");
        dsSelWithoutProcess.setDatasetLabel("no process");
        dsSelRepo.saveAndFlush(dsSelWithoutProcess);

        BasketDatasetSelection dsSelWithProcess = new BasketDatasetSelection();
        dsSelWithProcess.setDatasetIpid("some ip id");
        dsSelWithProcess.setDatasetLabel("process");
        dsSelWithProcess.setProcessDatasetDescription(new ProcessDatasetDescription(processBusinessId, new HashMap<>()));
        Long withProcessId = dsSelRepo.saveAndFlush(dsSelWithProcess).getId();

        List<BasketDatasetSelection> result = dsSelRepo.findByProcessBusinessId(processBusinessId.toString());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(withProcessId);
    }

}