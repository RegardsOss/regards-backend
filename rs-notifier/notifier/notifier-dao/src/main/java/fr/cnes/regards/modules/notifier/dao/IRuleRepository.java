/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.notifier.dao;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.notifier.domain.Rule;

/**
 * Repository to manipulate {@link Rule}
 * @author Kevin Marchois
 *
 */
@Repository
public interface IRuleRepository extends JpaRepository<Rule, Long> {

    /**
     * Get all enabled {@link Rule}
     * @return a set of {@link Rule}
     */
    @EntityGraph(attributePaths = { "rulePlugin", "recipients" })
    public Set<Rule> findByRulePluginActiveTrue();

    @EntityGraph(attributePaths = { "rulePlugin", "recipients" })
    public Optional<Rule> findByRulePluginBusinessId(String businessId);

    @EntityGraph(attributePaths = { "recipients" })
    public Set<Rule> findByRecipientsBusinessId(String businessId);

    public void deleteByRulePluginBusinessId(String businessId);
}
