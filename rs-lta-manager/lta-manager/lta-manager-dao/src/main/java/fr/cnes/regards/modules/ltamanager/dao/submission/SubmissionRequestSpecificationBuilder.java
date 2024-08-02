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
package fr.cnes.regards.modules.ltamanager.dao.submission;

import fr.cnes.regards.framework.jpa.utils.AbstractSpecificationsBuilder;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest;
import fr.cnes.regards.modules.ltamanager.domain.submission.search.SearchSubmissionRequestParameters;

/**
 * @author Iliana Ghazali
 **/
public class SubmissionRequestSpecificationBuilder
    extends AbstractSpecificationsBuilder<SubmissionRequest, SearchSubmissionRequestParameters> {

    @Override
    protected void addSpecificationsFromParameters() {
        if (parameters != null) {
            specifications.add(equals("owner", parameters.getOwner()));
            specifications.add(equals("session", parameters.getSession()));
            specifications.add(useValuesRestriction("id", parameters.getIdsRestriction()));
            specifications.add(equals("submittedProduct.datatype", parameters.getDatatype()));
            specifications.add(useDatesRestriction("submissionStatus.creationDate", parameters.getCreationDate()));
            specifications.add(useDatesRestriction("submissionStatus.statusDate", parameters.getStatusDate()));
            specifications.add(useValuesRestriction("submissionStatus.status", parameters.getStatusesRestriction()));
        }
    }

}
