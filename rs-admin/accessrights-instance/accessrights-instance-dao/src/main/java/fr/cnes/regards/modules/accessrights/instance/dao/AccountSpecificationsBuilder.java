/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.instance.dao;

import fr.cnes.regards.framework.jpa.utils.AbstractSpecificationsBuilder;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountSearchParameters;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class AccountSpecificationsBuilder extends AbstractSpecificationsBuilder<Account, AccountSearchParameters> {

    @Override
    protected void addSpecificationsFromParameters() {
        if (parameters != null) {
            specifications.add(like("email", parameters.getEmail()));
            specifications.add(like("lastName", parameters.getLastName()));
            specifications.add(like("firstName", parameters.getFirstName()));
            specifications.add(hasStatus(parameters.getStatus()));
            specifications.add(equals("origin", parameters.getOrigin()));
            specifications.add(joinedEquals("projects", "name", parameters.getProject()));
        }
    }

    private Specification<Account> hasStatus(String status) {
        if (StringUtils.isEmpty(status)) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), AccountStatus.valueOf(status));
        }
    }

}
