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

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import javax.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.notifier.domain.Rule;
import fr.cnes.regards.modules.notifier.dto.RuleDTO;

/**
 * @author kevin
 *
 */
public interface IRuleService {

    public Page<RuleDTO> getRules(Pageable page);

    /**
     * Create or update a {@link Rule} from a {@link RuleDto}
     * @param toCreate
     * @return {@link RuleDto} from the created {@link Rule}
     * @throws ModuleException if during an update id is unknow
     */
    public RuleDTO createOrUpdateRule(@Valid RuleDTO toCreate) throws ModuleException;

    /**
     * Delete a {@link Rule} by its id.  <br/>
     * @param id
     * @throws ModuleException
     */
    public void deleteRule(String id) throws ModuleException;

    /**
     * @param id
     */
    public Optional<RuleDTO> getRule(String id);

    /**
     * Delete all existing rules configured
     * @return plugin business ids to delete
     */
    public Set<String> deleteAll(Collection<String> deletionErrors);
}
