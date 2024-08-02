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
package fr.cnes.regards.framework.test.integration;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.util.Strings;
import org.springframework.test.context.ActiveProfilesResolver;

import java.util.List;

/**
 * Resolver that con be used to add the test profile to an IT without overwriting the
 * profiles defined in the system (local-test or ci).
 *
 * @author Thibaud Michaudel
 **/
public class RegardsActiveProfileResolver implements ActiveProfilesResolver {

    public static final String TEST_PROFILE = "test";

    @Override
    public String[] resolve(Class<?> aClass) {
        List<String> springProfiles = Lists.newArrayList(TEST_PROFILE);
        String systemSpringProfile = System.getProperty("spring_profiles_active");
        if (Strings.isNotBlank(systemSpringProfile)) {
            springProfiles.add(systemSpringProfile);
        }
        return springProfiles.toArray(String[]::new);
    }
}
