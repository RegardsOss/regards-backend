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
package fr.cnes.regards.modules.ingest.domain.request.update;

import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/**
 * @author Léo Mieulet
 */
@Entity(name = "UpdateCategoryAIPTask")

public class AIPUpdateCategoryTask extends AbstractAIPUpdateTask {

    @Column(columnDefinition = "jsonb", name = "payload")
    @Type(value = JsonBinaryType.class,
          parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private List<String> categories;

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public static AIPUpdateCategoryTask build(AIPUpdateTaskType type, AIPUpdateState state, List<String> categories) {
        AIPUpdateCategoryTask task = new AIPUpdateCategoryTask();
        task.setType(type);
        task.setCategories(categories);
        task.setState(state);
        return task;
    }
}
