/*
 * Copyright 2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.dao;

import java.math.BigInteger;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.storage.domain.database.AIPEntity;

/**
 * This class allows to fetch tags using native SQL
 * @author Léo Mieulet
 */
@Repository
public class CustomizedAIPEntityRepository implements ICustomizedAIPEntityRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<String> getDistinctTags(String sql) {
        Query q = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<String> resultList = q.getResultList();
        return resultList;
    }

    @Override
    public Page<AIPEntity> findAll(String sqlQuery, Pageable pageable) {
        Long numberResults = countNumberOfResults(sqlQuery);
        Query q = entityManager.createNativeQuery(sqlQuery, AIPEntity.class);
        // Handle the pagination here
        q.setFirstResult(pageable.getOffset());
        q.setMaxResults(pageable.getPageSize());
        @SuppressWarnings("unchecked")
        List<AIPEntity> resultList = q.getResultList();
        Page<AIPEntity> result = new PageImpl<>(resultList, pageable, numberResults);
        return result;
    }

    private Long countNumberOfResults(String sqlQuery) {
        StringBuilder request = new StringBuilder("SELECT COUNT(*) as total FROM (").append(sqlQuery)
                .append(") as sub");
        Query qCount = entityManager.createNativeQuery(request.toString());
        Long totalResults = ((BigInteger) qCount.getSingleResult()).longValue();
        return totalResults;
    }
}
