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
package fr.cnes.regards.modules.ingest.domain;

import javax.validation.Valid;

import fr.cnes.regards.framework.oais.AbstractInformationPackage;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;
import fr.cnes.regards.modules.ingest.domain.validator.CheckSIP;
import fr.cnes.regards.modules.ingest.domain.validator.CheckSIPId;

/**
 *
 * SIP representation based on OAIS information package standard structure as well as GeoJson structure.<br/>
 * Base representation is used for SIP passed by value.<br/>
 * "ref" extension attribute is used for SIP passed by reference.<br/>
 *
 * To build a {@link SIP}, you have to use a {@link SIPBuilder}.
 *
 * @author Marc Sordi
 *
 */
@CheckSIP(message = "The SIP must be sent either by reference or by value")
@CheckSIPId(message = "The SIP identifier is required")
public class SIP extends AbstractInformationPackage<String> {

    /**
     * ref : extension attribute for SIP passed by reference. Should be null if SIP passed by value.
     */
    @Valid
    private SIPReference ref;

    public SIPReference getRef() {
        return ref;
    }

    public void setRef(SIPReference ref) {
        this.ref = ref;
    }

    public boolean isRef() {
        return ref != null;
    }
}
