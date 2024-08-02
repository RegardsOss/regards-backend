/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.configuration.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

/**
 * Entity to describe a search history
 *
 * @author Théo Lasserre
 */
@Entity
@Table(name = "t_search_history",
       uniqueConstraints = @UniqueConstraint(name = "uk_search_history_name", columnNames = "name"))
public class SearchHistory {

    /**
     * Unique id
     */
    @Id
    @SequenceGenerator(name = "searchHistorySequence", initialValue = 1, sequenceName = "seq_search_history")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "searchHistorySequence")
    private Long id;

    /**
     * Search history name
     */
    @NotNull
    @Column(name = "name", nullable = false, length = 16)
    private String name;

    /**
     * JSON object. Holds multiple search criterion.
     */
    @NotNull
    @Column(name = "configuration", nullable = false, columnDefinition = "text")
    private String configuration;

    /**
     * Unique identifier of an account email.
     */
    @NotNull
    @Column(name = "account_email", nullable = false)
    private String accountEmail;

    /**
     * Unique identifier of a module id.
     */
    @NotNull
    @Column(name = "module_id", nullable = false)
    private Long moduleId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public String getAccountEmail() {
        return accountEmail;
    }

    public void setAccountEmail(String accountEmail) {
        this.accountEmail = accountEmail;
    }

    public Long getModuleId() {
        return moduleId;
    }

    public void setModuleId(Long moduleId) {
        this.moduleId = moduleId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SearchHistory other = (SearchHistory) obj;
        if (id == null) {
            return other.id == null;
        } else {
            return id.equals(other.id);
        }
    }

    @Override
    public String toString() {
        return String.format("SearchHistory [id=%s, name=%s, configuration=%s, account_email=%s, module_id=%s]",
                             id,
                             name,
                             configuration,
                             accountEmail,
                             moduleId);
    }
}
