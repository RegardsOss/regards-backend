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

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;
import java.util.Set;

/**
 * Interface for a JPA auto-generated CRUD repository managing {@link Account}s.<br>
 * Embeds paging/sorting abilities by entending {@link PagingAndSortingRepository}.<br>
 * Allows execution of Query by Example {@link Example} instances.
 *
 * @author Xavier-Alexandre Brochard
 * @author Sylvain Vissiere-Guerinet
 */
@InstanceEntity
public interface IAccountRepository extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {

    /**
     * Find the single {@link Account} with passed <code>email</code>.<br>
     * Custom query auto-implemented by JPA thanks to the method naming convention.
     *
     * @param pEmail
     *            The {@link Account}'s <code>email</code>
     * @return An optional account
     */
    Optional<Account> findOneByEmail(String pEmail);

    /**
     * Find all Account which status is not the one provided.
     * @param pStatus the status we do not want
     * @return the set of matching accounts
     */
    Set<Account> findAllByStatusNot(AccountStatus pStatus);

    /**
     * Find all Account which status is the one provided.
     * @param pStatus the status we want
     * @param pPageable the pageable object used by Spring for building the page of result
     * @return the page of matching accounts
     */
    Page<Account> findAllByStatus(AccountStatus pStatus, Pageable pPageable);

}
