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
package fr.cnes.regards.modules.notifier.service;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.notifier.domain.Rule;
import fr.cnes.regards.modules.notifier.dto.RuleDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.validation.Valid;
import java.util.Optional;

/**
 * @author kevin
 */
public interface IRuleService {

    Page<RuleDTO> getRules(Pageable page);

    /**
     * Create or update a {@link Rule} from a {@link RuleDto}
     *
     * @return {@link RuleDto} from the created {@link Rule}
     * @throws ModuleException if during an update id is unknow
     */
    RuleDTO createOrUpdate(@Valid RuleDTO toCreate) throws ModuleException;

    /**
     * Delete a {@link Rule} by its id.  <br/>
     */
    void delete(String id) throws ModuleException;

    /**
     *
     */
    Optional<RuleDTO> getRule(String id);

    /**
     * Delete all existing rules configured
     *
     * @return plugin business ids to delete
     */
    void deleteAll() throws ModuleException;

    /**
     * A recipient has just been updated.
     * Update cache in consequence.
     *
     * @param recipientId business id of the delete recipent.
     */
    void recipientUpdated(String recipientId);

    /**
     * A recipient has just been deleted.
     * Remove it from rules.
     *
     * @param recipientId business id of the delete recipent.
     */
    void recipientDeleted(String recipientId);
}
