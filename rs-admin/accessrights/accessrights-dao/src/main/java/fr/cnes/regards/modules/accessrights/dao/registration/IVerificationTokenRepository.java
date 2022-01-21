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
package fr.cnes.regards.modules.accessrights.dao.registration;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import fr.cnes.regards.modules.accessrights.domain.emailverification.EmailVerificationToken;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * Interface for a JPA auto-generated CRUD repository managing {@link EmailVerificationToken}s.<br>
 *
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 */
public interface IVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    /**
     * Find token with given string
     * @param pToken the string
     * @return the optional token
     */
    Optional<EmailVerificationToken> findByToken(String pToken);

    /**
     * Find token with given ProjectUser
     * @param pProjectUser the project user
     * @return the option token
     */
    Optional<EmailVerificationToken> findByProjectUser(ProjectUser pProjectUser);

    /**
     * Find all token with expiry date less than given date
     * @param pNow the given date
     * @return a stream of matching tokens
     */
    Stream<EmailVerificationToken> findAllByExpiryDateLessThan(LocalDateTime pNow);

    /**
     * Delete all tokens with expiry date less than given date
     * @param pNow the fien date
     */
    void deleteByExpiryDateLessThan(LocalDateTime pNow);

    /**
     * Delete all token which expired since given date
     * @param pNow the given date
     */
    @Modifying
    @Query("delete from EmailVerificationToken t where t.expiryDate <= ?1")
    void deleteAllExpiredSince(LocalDateTime pNow);
}