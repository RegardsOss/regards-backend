/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.storage.dao.IAIPEntityRepository;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;

/**
 * @author Marc SORDI
 *
 */
@Service
@MultitenantTransactional
public class TransactionalService {

    @Autowired
    private EntityManager em;

    @Autowired
    private IAIPEntityRepository aipEntityRepo;

    public Page<AIPEntity> search(AIPState state) {
        Page<AIPEntity> page = aipEntityRepo.findAllByState(state, new PageRequest(0, 100, Direction.ASC, "id"));
        return page;
    }

    public List<AIPEntity> findFirst100(AIPState state) {
        List<AIPEntity> first100 = aipEntityRepo.findFirst100ByState(state);
        return first100;
    }

    public AIPEntity update(AIPEntity entity) {
        aipEntityRepo.save(entity);
        em.flush();
        em.clear();
        return entity;
    }
}
