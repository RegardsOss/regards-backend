/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.client.utils;

import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;

import java.util.UUID;

/**
 * @author Iliana Ghazali
 **/
public class OrderTestConstants {

    public static final String USER_EMAIL = "user-order-client@test.eu";

    public static final String USER_ROLE = DefaultRole.EXPLOIT.name();

    // DATASET IDS
    public static final UniformResourceName DS1_IP_ID = UniformResourceName.build(OAISIdentifier.AIP,
                                                                                  EntityType.DATASET,
                                                                                  "ORDER",
                                                                                  UUID.randomUUID(),
                                                                                  1);

    public static final UniformResourceName DS2_IP_ID = UniformResourceName.build(OAISIdentifier.AIP,
                                                                                  EntityType.DATASET,
                                                                                  "ORDER",
                                                                                  UUID.randomUUID(),
                                                                                  1);

    // DATA OBJECT IDS (AIPS)
    public static final UniformResourceName DO1_IP_ID = UniformResourceName.build(OAISIdentifier.AIP,
                                                                                  EntityType.DATA,
                                                                                  "ORDER",
                                                                                  UUID.randomUUID(),
                                                                                  1);

    public static final UniformResourceName DO2_IP_ID = UniformResourceName.build(OAISIdentifier.AIP,
                                                                                  EntityType.DATA,
                                                                                  "ORDER",
                                                                                  UUID.randomUUID(),
                                                                                  1);

    public static final UniformResourceName DO3_IP_ID = UniformResourceName.build(OAISIdentifier.AIP,
                                                                                  EntityType.DATA,
                                                                                  "ORDER",
                                                                                  UUID.randomUUID(),
                                                                                  1);

    public static final UniformResourceName DO4_IP_ID = UniformResourceName.build(OAISIdentifier.AIP,
                                                                                  EntityType.DATA,
                                                                                  "ORDER",
                                                                                  UUID.randomUUID(),
                                                                                  1);

    public static final UniformResourceName DO5_IP_ID = UniformResourceName.build(OAISIdentifier.AIP,
                                                                                  EntityType.DATA,
                                                                                  "ORDER",
                                                                                  UUID.randomUUID(),
                                                                                  1);
}
