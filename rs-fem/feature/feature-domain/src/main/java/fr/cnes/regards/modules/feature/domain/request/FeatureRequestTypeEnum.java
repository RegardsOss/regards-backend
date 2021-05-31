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
package fr.cnes.regards.modules.feature.domain.request;

/**
 * Enumeration of possible discriminent type for {@link AbstractFeatureRequest}
 *
 * @author Sébastien Binda
 *
 */
public enum FeatureRequestTypeEnum {

    COPY(FeatureRequestTypeEnum.COPY_DISCRIMINENT),

    UPDATE(FeatureRequestTypeEnum.UPDATE_DISCRIMINENT),

    NOTIFICATION(FeatureRequestTypeEnum.NOTIFICATION_DISCRIMINENT),

    CREATION(FeatureRequestTypeEnum.CREATION_DISCRIMINENT),

    DELETION(FeatureRequestTypeEnum.DELETION_DISCRIMINENT),

    SAVE_METADATA(FeatureRequestTypeEnum.SAVE_METADATA_DISCRIMINENT);

    public static final String COPY_DISCRIMINENT = "COPY";

    public static final String UPDATE_DISCRIMINENT = "UPDATE";

    public static final String NOTIFICATION_DISCRIMINENT = "NOTIFICATION";

    public static final String CREATION_DISCRIMINENT = "CREATION";

    public static final String DELETION_DISCRIMINENT = "DELETION";

    public static final String SAVE_METADATA_DISCRIMINENT = "SAVE_METADATA";

    private String discriminent;

    FeatureRequestTypeEnum(String discriminent) {
        this.discriminent = discriminent;
    }

    public String getDiscriminent() {
        return discriminent;
    }

}
