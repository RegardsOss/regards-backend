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
package fr.cnes.regards.modules.accessrights.instance.dao.accountunlock;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.accountunlock.AccountUnlockToken;

/**
 * Interface for a JPA auto-generated CRUD repository managing {@link AccountUnlockToken}s.<br>
 *
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 */
@InstanceEntity
public interface IAccountUnlockTokenRepository extends JpaRepository<AccountUnlockToken, Long> {

    Optional<AccountUnlockToken> findByToken(String pToken);

    Optional<AccountUnlockToken> findByAccount(Account pAccount);

    Stream<AccountUnlockToken> findAllByExpiryDateLessThan(LocalDateTime pNow);

    void deleteByExpiryDateLessThan(LocalDateTime pNow);

    @Modifying
    @Query("delete from AccountUnlockToken t where t.expiryDate <= ?1")
    void deleteAllExpiredSince(LocalDateTime pNow);

    /**
     * Delete all {@link AccountUnlockToken}s for the passed {@link Account}
     *
     * @param pAccount
     *            the account
     */
    void deleteAllByAccount(Account pAccount);
}