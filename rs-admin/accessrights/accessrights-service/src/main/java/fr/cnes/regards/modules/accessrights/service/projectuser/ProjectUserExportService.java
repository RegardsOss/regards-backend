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
package fr.cnes.regards.modules.accessrights.service.projectuser;

import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUserSearchParameters;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;

@Service
@MultitenantTransactional
public class ProjectUserExportService {

    private static final String INNER_SEPARATOR = ",";
    private static final String COLUMN_SEPARATOR = ";";
    private static final String HEADER = String.join(
            COLUMN_SEPARATOR,
            "USER_ID",
            "EMAIL",
            "FIRST_NAME",
            "LAST_NAME",
            "STATUS",
            "ROLE",
            "ORIGIN",
            "ACCESS_GROUPS",
            "MAX_QUOTA",
            "CURRENT_QUOTA",
            "METADATA",
            "LICENSE_ACCEPTED",
            "CREATION_DATE",
            "LAST_CONNECTION",
            "LAST_UPDATE");

    private final IProjectUserService projectUserService;

    public ProjectUserExportService(IProjectUserService projectUserService) {
        this.projectUserService = projectUserService;
    }

    public void export(BufferedWriter writer, ProjectUserSearchParameters parameters) throws IOException {
        writer.append(HEADER);
        writer.newLine();
        for (ProjectUser projectUser : projectUserService.retrieveUserList(parameters, Pageable.unpaged())) {
            writeLine(writer, projectUser);
        }
        writer.close();
    }

    private void writeLine(BufferedWriter writer, ProjectUser projectUser) throws IOException {

        // Order is important here, must match HEADER

        addValue(writer, projectUser.getId());
        addValue(writer, projectUser.getEmail());
        addValue(writer, projectUser.getFirstName());
        addValue(writer, projectUser.getLastName());
        addValue(writer, projectUser.getStatus().toString());
        addValue(writer, projectUser.getRole().getName());
        addValue(writer, projectUser.getOrigin());

        String groups = null;
        if (!CollectionUtils.isEmpty(projectUser.getAccessGroups())) {
            groups = String.join(INNER_SEPARATOR, projectUser.getAccessGroups());
        }
        addValue(writer, groups);

        addValue(writer, projectUser.getMaxQuota());
        addValue(writer, projectUser.getCurrentQuota());

        String metaData = projectUser.getMetadata()
                                     .stream()
                                     .filter(ProjectUserService.KEEP_VISIBLE_META_DATA)
                                     .map(data -> data.getKey() + "=" + data.getValue())
                                     .collect(Collectors.joining(INNER_SEPARATOR));
        addValue(writer, metaData);

        addValue(writer, projectUser.isLicenseAccepted());
        addValue(writer, projectUser.getCreated());
        addValue(writer, projectUser.getLastConnection());
        addValue(writer, projectUser.getLastUpdate());

        writer.newLine();
    }

    private void addValue(BufferedWriter writer, Object value) throws IOException {
        if (value instanceof String) {
            writer.append("\"");
            writer.append(((String) value));
            writer.append("\"");
        } else if (value instanceof OffsetDateTime) {
            writer.append(OffsetDateTimeAdapter.format((OffsetDateTime) value));
        } else {
            writer.append(String.valueOf(value));
        }
        writer.append(COLUMN_SEPARATOR);
    }

}
