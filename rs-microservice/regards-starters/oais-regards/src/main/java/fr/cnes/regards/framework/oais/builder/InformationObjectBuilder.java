/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.oais.builder;

import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.oais.InformationObject;
import fr.cnes.regards.framework.oais.PreservationDescriptionInformation;

/**
 * Information object builder
 *
 * @author Marc Sordi
 *
 */
public class InformationObjectBuilder implements IOAISBuilder<InformationObject> {

    private final InformationObject io = new InformationObject();

    private final ContentInformationBuilder contentInformationBuilder = new ContentInformationBuilder();

    private final PDIBuilder pdiBuilder = new PDIBuilder();

    @Override
    public InformationObject build() {
        io.setContentInformation(contentInformationBuilder.build());
        io.setPdi(pdiBuilder.build());
        return io;
    }

    /**
     * @return builder for building <b>required</b> {@link ContentInformation}
     */
    public ContentInformationBuilder getContentInformationBuilder() {
        return contentInformationBuilder;
    }

    /**
     * @return builder for <b>required</b> {@link PreservationDescriptionInformation}
     */
    public PDIBuilder getPDIBuilder() {
        return pdiBuilder;
    }
}
