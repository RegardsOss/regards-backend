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
package fr.cnes.regards.modules.ingest.domain.request.update;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.util.List;

/**
 * @author Léo Mieulet
 */
@Entity(name = "UpdateTagAIPTask")
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
public class AIPUpdateTagTask extends AbstractAIPUpdateTask {

    @Column(columnDefinition = "jsonb", name = "payload")
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private List<String> tags;

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public static AIPUpdateTagTask build(AIPUpdateTaskType type, AIPUpdateState state, List<String> tags) {
        AIPUpdateTagTask task = new AIPUpdateTagTask();
        task.setType(type);
        task.setTags(tags);
        task.setState(state);
        return task;
    }
}
