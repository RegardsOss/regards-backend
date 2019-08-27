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
package fr.cnes.regards.modules.ingest.dao;


import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.utils.CustomPostgresDialect;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specification class to filter DAO searches on {@link AIPEntity} entities
 * @author Léo Mieulet
 */
public class AIPSpecification {

    public static Specification<AIPEntity> searchAll(AIPState state, OffsetDateTime from, OffsetDateTime to,
            List<String> tags, String sessionOwner, String session, String providerId, List<String> storages) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            if (tags != null && !tags.isEmpty()) {
                // Create an empty array
                Expression<List> tagsContraint = cb.function(CustomPostgresDialect.EMPTY_STRING_ARRAY, List.class);
                for (String tag : tags) {
                    // Append to that array every tag
                    tagsContraint = cb.function("array_append", List.class,
                            tagsContraint,
                            cb.function(CustomPostgresDialect.STRING_LITERAL, String.class, cb.literal(tag))
                    );
                }
                // use the jsonb_exists_all (?&) operator
                Expression<Boolean> function = cb.function("jsonb_exists_all", Boolean.class, root.get("tags"),
                        tagsContraint
                );
                predicates.add(cb.isTrue(function));
            }

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

}