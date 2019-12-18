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

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Set;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.utils.SpecificationUtils;
import fr.cnes.regards.modules.storage.domain.database.FileReference;

/**
 * Specification class to filter DAO searches on {@link FileReference} entities
 *
 * @author Sébastien Binda
 */
public class FileReferenceSpecification {

    private FileReferenceSpecification() {
    }

    public static Specification<FileReference> search(String fileName, String checksum, Collection<String> types,
            Collection<String> storages, Collection<String> owners, OffsetDateTime from, OffsetDateTime to,
            Pageable page) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            Join<Object, Object> metaInfoJoin = root.join("metaInfo");
            if (checksum != null) {
                predicates.add(cb.equal(root.get("metaInfo").get("checksum"), checksum));
                predicates.add(cb.equal(metaInfoJoin.get("checksum"), checksum));
            }
            if (fileName != null) {
                predicates.add(cb.like(metaInfoJoin.get("fileName"), "%" + fileName + "%"));
            }

            if ((types != null) && !types.isEmpty()) {
                predicates.add(metaInfoJoin.get("type").in(types));
            }
            if ((storages != null) && !storages.isEmpty()) {
                predicates.add(root.get("location").get("storage").in(storages));
            }
            if ((owners != null) && !owners.isEmpty()) {
                predicates.add(root.get("owners").in(owners));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("storageDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("storageDate"), to));
            }
            // Add order
            Sort.Direction defaultDirection = Sort.Direction.ASC;
            String defaultAttribute = "id";
            query.orderBy(SpecificationUtils.buildOrderBy(page, root, cb, defaultAttribute, defaultDirection));

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }
}