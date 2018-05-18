/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.plugins.ssalto;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

/**
 * 
 * @author Christophe Mertz
 *
 */
public class PatternTest extends TestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(PatternTest.class);

    @Test
    public void testPattern() {

        String patternString = " ([0-9]{2}) *([0-9]{1,2}) *([0-9]{1,2}) *([0-9]{1,2}) *([0-9]{1,2}) *([0-9]{1,2})\\.(.)*";
        String fileName = " 09 04 19 22 29 40.0000000 0 10 01 02 05 09 12 14 24 27 29 30";
        
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(fileName);
        
        if (!matcher.matches()) {
            fail();
        }

        LOGGER.info("matches ? " + matcher.matches());
        for (int i = 1; i <= matcher.groupCount(); i++) {
            LOGGER.info("group " + i + " is : " + matcher.group(i));
            LOGGER.info("group " + i + " is : " + Integer.parseInt(matcher.group(i)));
        }
    }
}
