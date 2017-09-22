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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.tools;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.acquisition.tools.CNESJulianDate;
import fr.cnes.regards.modules.acquisition.tools.DateFormatter;

/**
 * 
 * @author Christophe Mertz
 *
 */
public class CNESJulianDateTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CNESJulianDateTest.class);

    /*
     * Class under test for Date toDate(Integer, Integer)
     */
    @Test
    public void testToDateIntegerInteger() {
        Integer julianDays = new Integer(20076);
        Integer secondInDay = new Integer(86400);

        Date tmp = CNESJulianDate.toDate(julianDays, secondInDay);
        LOGGER.info("Resultat Date Julien= "
                + DateFormatter.getDateRepresentation(tmp, DateFormatter.XS_DATE_TIME_FORMAT));

        Assert.assertTrue(DateFormatter.getDateRepresentation(tmp, DateFormatter.XS_DATE_TIME_FORMAT)
                .equals("2004-12-19T23:59:59"));
    }

}
