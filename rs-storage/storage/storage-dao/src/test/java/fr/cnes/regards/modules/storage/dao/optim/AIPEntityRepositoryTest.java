/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.dao.optim;

import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTest;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.storage.dao.DAOTestConfiguration;
import fr.cnes.regards.modules.storage.dao.IAIPEntityRepository;
import fr.cnes.regards.modules.storage.dao.IAIPSessionRepository;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.AIPSessionBuilder;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;

/**
 * @author Marc SORDI
 *
 */
@Ignore
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=optimstorage" })
@ContextConfiguration(classes = DAOTestConfiguration.class)
public class AIPEntityRepositoryTest extends AbstractDaoTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPEntityRepositoryTest.class);

    @Autowired
    private IAIPEntityRepository aipEntityRepo;

    @Autowired
    private IAIPSessionRepository aipSessionRepository;

    @Autowired
    private TransactionalService search;

    @SuppressWarnings("unused")
    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Test
    public void findAllTest() {

        AIPSession session = aipSessionRepository.save(AIPSessionBuilder.build("DEFAULT_SESSION"));

        long bulk = 10;
        long maxloops = 1000;
        List<AIPEntity> entities = new ArrayList<>();
        List<AIPEntity> concurrentlyUpdatedEntities = new ArrayList<>();
        AIPState state = AIPState.VALID;
        for (long i = 0; i < maxloops; i++) {
            entities.add(create("AIP" + i, session, state));
            if (i % bulk == 0) {
                concurrentlyUpdatedEntities.add(entities.get(0));
                LOGGER.info("Save {} entities of {}", bulk, i);
                aipEntityRepo.saveAll(entities);
                entities = new ArrayList<>();
                state = state == AIPState.VALID ? AIPState.STORED : AIPState.VALID;
            }
        }
        if (!entities.isEmpty()) {
            aipEntityRepo.saveAll(entities);
        }

        // Launch concurrent transactions
        //        ConcurrentTasks tasks = new ConcurrentTasks(getDefaultTenant(), concurrentlyUpdatedEntities);
        //        beanFactory.autowireBean(tasks);
        //        tasks.start();

        List<AIPEntity> page = search.searchWithoutCount(AIPState.VALID);
        //List<AIPEntity> page = search.findFirst100(AIPState.VALID);
        Assert.assertNotNull(page);
    }

    @Test
    public void findAllOnlyTest() {
        long startTime = System.currentTimeMillis();
        Page<AIPEntity> page = aipEntityRepo.findAllByState(AIPState.VALID,
                                                            PageRequest.of(0, 100, Direction.ASC, "id"));
        LOGGER.info("Request took {}ms", System.currentTimeMillis() - startTime);
        Assert.assertNotNull(page);
    }

    private AIPEntity create(String providerId, AIPSession session, AIPState state) {
        UUID uuid = UUID.nameUUIDFromBytes(providerId.getBytes());
        UniformResourceName sipId = new UniformResourceName(OAISIdentifier.SIP, EntityType.DATA, getDefaultTenant(),
                uuid, 1);

        UniformResourceName aipId = new UniformResourceName(OAISIdentifier.AIP, sipId.getEntityType(),
                sipId.getTenant(), sipId.getEntityId(), sipId.getVersion());

        AIPBuilder builder = new AIPBuilder(aipId, Optional.of(sipId), providerId, EntityType.DATA, session.getId());

        builder.getContentInformationBuilder().setDataObject(DataType.RAWDATA,
                                                             Paths.get("src", "main", "test", "resources", "data",
                                                                       "cdpp_collection.json"),
                                                             "MD5", "azertyuiopqsdfmlmld");
        builder.getContentInformationBuilder().setSyntax(MediaType.APPLICATION_JSON_UTF8);
        builder.addContentInformation();

        // Add creation event
        builder.addEvent(EventType.SUBMISSION.name(), "submission", OffsetDateTime.now());
        builder.addEvent(String.format("AIP %s generated", providerId));

        AIPEntity aipEntity = new AIPEntity(builder.build(), session);
        aipEntity.setState(state);
        return aipEntity;
    }
}
