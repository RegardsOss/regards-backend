/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
*/
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