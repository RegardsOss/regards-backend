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
package fr.cnes.regards.modules.ingest.dao;

import fr.cnes.regards.modules.ingest.domain.dto.NativeSelectQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TemporalType;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * This class allows to fetch tags using native SQL
 *
 * @author Léo Mieulet
 */
@Repository
public class CustomAIPRepository implements ICustomAIPRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<String> getDistinct(NativeSelectQuery query) {
        Query q = entityManager.createNativeQuery(query.getSQL());
        // Add params
        Map<String, String> params = query.getParams();
        for (String paramKey : params.keySet()) {
            q.setParameter(paramKey, params.get(paramKey));
        }
        // Add date params
        Map<String, Date> dateParams = query.getDateParams();
        for (String paramKey : dateParams.keySet()) {
            q.setParameter(paramKey, dateParams.get(paramKey), TemporalType.TIMESTAMP);
        }
        @SuppressWarnings("unchecked") List<String> resultList = q.getResultList();
        return resultList;
    }
}
