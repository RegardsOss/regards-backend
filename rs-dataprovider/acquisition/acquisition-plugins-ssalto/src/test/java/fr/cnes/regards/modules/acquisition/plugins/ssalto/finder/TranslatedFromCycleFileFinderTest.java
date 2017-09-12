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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.finder;

import java.io.File;
import java.io.FileFilter;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.finder.TranslatedFromCycleFileFinder;
import fr.cnes.regards.modules.acquisition.plugins.properties.PluginConfigurationProperties;

public class TranslatedFromCycleFileFinderTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TranslatedFromCycleFileFinderTest.class);

    private static final String ATT_NAME = "ATT_NAME";

    private static final String START_DATE = "START_DATE";

    private static final String STOP_DATE = "STOP_DATE";

    //    private static final Date ATT_VALUE_DATE_INSIDE = new Date();// (108, 9, 10);

    //    private static final Integer ATT_VALUE_CYCLE = new Integer(3);

    //    private static final Integer ATT_VALUE_CYCLE_OVER = new Integer(3000);

    private static final String JA2_CYCLE_FILEPATH = "src/test/resources/income/data/JASON2/CYCLES/JASON2_CYCLES";

    private static final String JA2_ORF_FILEPATH = "src/test/resources/income/data/JASON2/ORF_HISTORIQUE/JA2_ORF_AXXCN*";

    //    private static final String JA1_CYCLE_FILEPATH = "testing/ressources/ssalto/domain/plugins/impl/finder/JASON1_CYCLES";

    //    private static final String JA1_ORF_FILEPATH = "testing/ressources/ssalto/domain/plugins/impl/finder/JA1_ORF_AXXCN*";

    //    private static final String BEF_DRIFT_JA1_ORF_FILEPATH = "testing/ressources/ssalto/domain/plugins/impl/finder/BEF_DRIFT_JA1_ORF_AXXCN*";

    //    private static final Integer ATT_VALUE_CYCLE_000_FILE = new Integer(0);

    //    private static final Integer ATT_VALUE_CYCLE_FIRST_FILE = new Integer(262);

    //    private static final Integer ATT_VALUE_CYCLE_UNKNOWN = new Integer(999);

    @Test
    public void jasonGetCycleFromDateInsideCycleInterval() throws Exception {
        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
        translatedFromCycleFileFinder.setAttributProperties(jasonInitConfProperties());
        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
        translatedFromCycleFileFinder.setValueType(AttributeTypeEnum.TYPE_INTEGER.toString());

        Map<String, List<? extends Object>> attributMap = new HashMap<>();
        List<OffsetDateTime> attValueList = new ArrayList<>();
        LocalDateTime ldt = LocalDateTime.of(2010, 9, 9, 0, 31);
        attValueList.add(OffsetDateTime.of(ldt, ZoneOffset.UTC));
        attributMap.put(ATT_NAME, attValueList);

        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
        LOGGER.info("CYCLE : " + resultList.get(0).toString());

        Assert.assertEquals(1, resultList.size());
        Assert.assertEquals(320, ((Integer) resultList.get(0)).intValue());
    }

    @Test
    public void jasonGetCycleFromDateOverCycleInterval() throws Exception {
        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
        translatedFromCycleFileFinder.setAttributProperties(jasonInitConfProperties());
        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
        translatedFromCycleFileFinder.setValueType(AttributeTypeEnum.TYPE_INTEGER.toString());

        Map<String, List<? extends Object>> attributMap = new HashMap<>();
        List<OffsetDateTime> attValueList = new ArrayList<>();
        LocalDateTime ldt = LocalDateTime.of(2012, 3, 4, 10, 25);
        attValueList.add(OffsetDateTime.of(ldt, ZoneOffset.UTC));
        attributMap.put(ATT_NAME, attValueList);

        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
        LOGGER.info("CYCLE : " + resultList.get(0).toString());

        Assert.assertEquals(1, resultList.size());
        Assert.assertEquals(366, ((Integer) resultList.get(0)).intValue());
    }

    @Test
    public void jason2GetCycleFromDateOverCycleInterval() throws Exception {
        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
        translatedFromCycleFileFinder.setAttributProperties(jason2InitConfProperties());
        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
        translatedFromCycleFileFinder.setValueType(AttributeTypeEnum.TYPE_INTEGER.toString());

        Map<String, List<? extends Object>> attributMap = new HashMap<>();
        List<OffsetDateTime> attValueList = new ArrayList<>();
        LocalDateTime ldt = LocalDateTime.of(2020, 1, 1, 0, 31);
        attValueList.add(OffsetDateTime.of(ldt, ZoneOffset.UTC));
        attributMap.put(ATT_NAME, attValueList);

        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
        Assert.assertEquals(1, resultList.size());
        Assert.assertEquals(126, ((Integer) resultList.get(0)).intValue());
    }

    @Test
    public void jason2GetCycleFromDateEqualCycleStartDate() throws Exception {
        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
        translatedFromCycleFileFinder.setAttributProperties(jason2InitConfProperties());
        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
        translatedFromCycleFileFinder.setValueType(AttributeTypeEnum.TYPE_INTEGER.toString());

        Map<String, List<? extends Object>> attributMap = new HashMap<>();
        List<OffsetDateTime> attValueList = new ArrayList<>();
        LocalDateTime ldt = LocalDateTime.of(2009, 5, 15, 10, 6);
        attValueList.add(OffsetDateTime.of(ldt, ZoneOffset.UTC));
        attributMap.put(ATT_NAME, attValueList);

        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
        Assert.assertEquals(1, resultList.size());
        Assert.assertEquals(31, ((Integer) resultList.get(0)).intValue());

        attValueList = new ArrayList<>();
        ldt = LocalDateTime.of(2009, 5, 15, 10, 35);
        attValueList.add(OffsetDateTime.of(ldt, ZoneOffset.UTC));
        attributMap.put(ATT_NAME, attValueList);

        resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
        Assert.assertEquals(1, resultList.size());
        Assert.assertEquals(32, ((Integer) resultList.get(0)).intValue());
    }

    @Test
    public void jason2GetCycleFromDateInsideCycleInterval() throws Exception {
        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
        translatedFromCycleFileFinder.setAttributProperties(jason2InitConfProperties());
        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
        translatedFromCycleFileFinder.setValueType(AttributeTypeEnum.TYPE_INTEGER.toString());

        Map<String, List<? extends Object>> attributMap = new HashMap<>();
        List<OffsetDateTime> attValueList = new ArrayList<>();
        LocalDateTime ldt = LocalDateTime.of(2010, 4, 4, 5, 46);
        attValueList.add(OffsetDateTime.of(ldt, ZoneOffset.UTC));
        attributMap.put(ATT_NAME, attValueList);

        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
        Assert.assertEquals(1, resultList.size());
        Assert.assertEquals(64, ((Integer) resultList.get(0)).intValue());
    }

    @Test(expected = PluginAcquisitionException.class)
    public void getDateFromCycleIncorrect() throws Exception {
        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
        translatedFromCycleFileFinder.setAttributProperties(jasonInitConfProperties());
        translatedFromCycleFileFinder.setName(START_DATE);
        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);

        Map<String, List<? extends Object>> attributMap = new HashMap<>();
        List<Integer> attValueList = new ArrayList<>();
        attValueList.add(new Integer(999));
        attributMap.put(ATT_NAME, attValueList);

        translatedFromCycleFileFinder.setValueType(AttributeTypeEnum.TYPE_DATE_TIME.toString());
        translatedFromCycleFileFinder.getValueList(null, attributMap);

        Assert.fail();
    }

    @Test
    public void getDateFromCycle() throws Exception {
        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
        translatedFromCycleFileFinder.setAttributProperties(jasonInitConfProperties());
        translatedFromCycleFileFinder.setName(START_DATE);
        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
        translatedFromCycleFileFinder.setValueType(AttributeTypeEnum.TYPE_DATE.toString());

        Map<String, List<? extends Object>> attributMap = new HashMap<>();
        List<Integer> attValueList = new ArrayList<>();
        attValueList.add(new Integer(300));
        attributMap.put(ATT_NAME, attValueList);

        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
        Assert.assertEquals(1, resultList.size());

        LocalDateTime expectedLdt = LocalDateTime.of(2010, 2, 22, 00, 56, 25, 534000000);
        Assert.assertTrue(OffsetDateTime.of(expectedLdt, ZoneOffset.UTC).isEqual((OffsetDateTime) resultList.get(0)));
    }

    @Test
    public void jason2GetDateFromCycleZero() throws Exception {
        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
        translatedFromCycleFileFinder.setAttributProperties(jason2InitConfProperties());
        translatedFromCycleFileFinder.setName(START_DATE);
        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
        translatedFromCycleFileFinder.setValueType(AttributeTypeEnum.TYPE_DATE.toString());

        Map<String, List<? extends Object>> attributMap = new HashMap<>();
        List<Integer> attValueList = new ArrayList<>();
        // recherche du cycle 0
        attValueList.add(new Integer(0));
        attributMap.put(ATT_NAME, attValueList);

        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
        Assert.assertEquals(1, resultList.size());

        LocalDateTime expectedLdt = LocalDateTime.of(2008, 7, 4, 5, 57, 7, 457000000);
        Assert.assertTrue(OffsetDateTime.of(expectedLdt, ZoneOffset.UTC).isEqual((OffsetDateTime) resultList.get(0)));

        translatedFromCycleFileFinder.setName(STOP_DATE);

        resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
        Assert.assertEquals(1, resultList.size());

        expectedLdt = LocalDateTime.of(2008, 7, 12, 0, 51, 58, 407000000);
        Assert.assertTrue(OffsetDateTime.of(expectedLdt, ZoneOffset.UTC).isEqual((OffsetDateTime) resultList.get(0)));
    }

    // Cycle inclu dans le premier fichier
    @Test
    public void jason2GetDateFromCycleFirstFile() throws Exception {
        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
        translatedFromCycleFileFinder.setAttributProperties(jason2InitConfProperties());
        translatedFromCycleFileFinder.setName(START_DATE);
        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
        translatedFromCycleFileFinder.setValueType(AttributeTypeEnum.TYPE_DATE.toString());

        Map<String, List<? extends Object>> attributMap = new HashMap<>();
        List<Integer> attValueList = new ArrayList<>();
        // recherche du cycle 1        
        attValueList.add(new Integer(1));
        attributMap.put(ATT_NAME, attValueList);

        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
        Assert.assertEquals(1, resultList.size());
        LocalDateTime expectedLdt = LocalDateTime.of(2008, 7, 12, 1, 20, 5, 48000000);
        Assert.assertTrue(OffsetDateTime.of(expectedLdt, ZoneOffset.UTC).isEqual((OffsetDateTime) resultList.get(0)));

        translatedFromCycleFileFinder.setName(STOP_DATE);
        resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
        Assert.assertEquals(1, resultList.size());
        expectedLdt = LocalDateTime.of(2008, 7, 21, 22, 50, 30, 178000000);
        Assert.assertTrue(OffsetDateTime.of(expectedLdt, ZoneOffset.UTC).isEqual((OffsetDateTime) resultList.get(0)));
    }

    // Cycle inclu dans le deuxieme fichier
    @Test
    public void jason2GetDateFromCycleSecondFile() throws Exception {
        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
        translatedFromCycleFileFinder.setAttributProperties(jason2InitConfProperties());
        translatedFromCycleFileFinder.setName(START_DATE);
        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
        translatedFromCycleFileFinder.setValueType(AttributeTypeEnum.TYPE_DATE.toString());

        Map<String, List<? extends Object>> attributMap = new HashMap<>();
        List<Integer> attValueList = new ArrayList<>();
        // recherche du cycle 28 qui est dans le fichier
        // JA2_ORF_AXXCNE20090624_122700_20080704_055707_20090702_080949
        attValueList.add(new Integer(28));
        attributMap.put(ATT_NAME, attValueList);

        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
        Assert.assertEquals(1, resultList.size());

        LocalDateTime expectedLdt = LocalDateTime.of(2009, 4, 5, 18, 40, 19, 209000000);
        Assert.assertTrue(OffsetDateTime.of(expectedLdt, ZoneOffset.UTC).isEqual((OffsetDateTime) resultList.get(0)));

        translatedFromCycleFileFinder.setName(STOP_DATE);
        resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
        Assert.assertEquals(1, resultList.size());

        expectedLdt = LocalDateTime.of(2009, 4, 15, 16, 10, 43, 594000000);
        Assert.assertTrue(OffsetDateTime.of(expectedLdt, ZoneOffset.UTC).isEqual((OffsetDateTime) resultList.get(0)));
    }

    // Cycle inclu dans aucun des fichiers
    @Test(expected = PluginAcquisitionException.class)
    public void jason2GetDateFromUnknownCycle() throws Exception {
        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
        translatedFromCycleFileFinder.setAttributProperties(jason2InitConfProperties());
        translatedFromCycleFileFinder.setName(START_DATE);
        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
        translatedFromCycleFileFinder.setValueType(AttributeTypeEnum.TYPE_DATE.toString());

        Map<String, List<? extends Object>> attributMap = new HashMap<>();
        List<Integer> attValueList = new ArrayList<>();
        // recherche du cycle 999 qui n'est dans aucun fichier
        attValueList.add(new Integer(999));
        attributMap.put(ATT_NAME, attValueList);

        translatedFromCycleFileFinder.getValueList(null, attributMap);
        Assert.fail();
    }

    //    // FIXME TEST @Test
    //    public void test_jasonGetCycleFromDate_equal_cycle_startDate() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(jasonInitConfProperties1());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        translatedFromCycleFileFinder.setValueType("INTEGER");
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Date> attValueList = new ArrayList<>();
    //
    //        GregorianCalendar calendar = new GregorianCalendar(2008, 7, 10); // = 2008,08,10
    //        calendar.set(Calendar.HOUR_OF_DAY, 0);
    //        calendar.set(Calendar.MINUTE, 31);
    //        calendar.set(Calendar.AM_PM, GregorianCalendar.AM);
    //
    //        attValueList.add(calendar.getTime());
    //        attributMap.put(ATT_NAME, attValueList);
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        System.out.println("CYCLE : " + resultList.get(0).toString());
    //        Assert.assertEquals(1, resultList.size());
    //        Assert.assertEquals(242, ((Integer) resultList.get(0)).intValue());
    //    }

    //    // FIXME TEST @Test
    //    public void test_getCycleFromDate_over_cycle_interval() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(initConfProperties());
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Date> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_DATE_OVER);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("INTEGER");
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Assert.assertEquals(17, ((Integer) resultList.get(0)).intValue());
    //    }
    //
    //    // FIXME TEST @Test
    //    public void test_getCycleFromDate_equal_cycle_startDate() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(initConfProperties());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        translatedFromCycleFileFinder.setValueType("INTEGER");
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Date> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_DATE_EQUAL);
    //        attributMap.put(ATT_NAME, attValueList);
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Assert.assertEquals(9, ((Integer) resultList.get(0)).intValue());
    //
    //        Calendar calendar = Calendar.getInstance();
    //        calendar.setTime(ATT_VALUE_DATE_EQUAL);
    //        calendar.set(Calendar.HOUR_OF_DAY, 8);
    //        attValueList = new ArrayList<>();
    //        attValueList.add(calendar.getTime());
    //        attributMap.put(ATT_NAME, attValueList);
    //        resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Assert.assertEquals(10, ((Integer) resultList.get(0)).intValue());
    //    }
    //
    //    // FIXME TEST @Test
    //    public void test_getCycleFromDate_inside_cycle_interval() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(initConfProperties());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Date> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_DATE_INSIDE);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("INTEGER");
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Assert.assertEquals(10, ((Integer) resultList.get(0)).intValue());
    //    }
    //
    //    // FIXME TEST @Test
    //    public void test_getDateFromCycle() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(initConfProperties());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Integer> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_CYCLE);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("DATE");
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Date dateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        Assert.assertEquals(sdf.format(dateResult), "2008/07/31 21:17:08.430", sdf.format(dateResult));
    //    }
    //
    //    // FIXME TEST @Test
    //    public void test_getDateFromCycle_incorrect() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(initConfProperties());
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Integer> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_CYCLE_OVER);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("DATE_TIME");
    //
    //            translatedFromCycleFileFinder.getValueList(null, attributMap);
    //            Assert.fail("must throw exception");
    //
    //    }

    //    // DM60 Gestion de plusieurs fichiers ORF
    //    // Cycle inclu dans le fichier orf cree
    //    // FIXME TEST @Test
    //    public void test_jasonGetDateFromCycle_000() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(jasonInitConfProperties1());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Integer> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_CYCLE_000_FILE);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("DATE");
    //        translatedFromCycleFileFinder.setName("START_DATE");
    //
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Date dateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("START_DATE : " + sdf.format(dateResult).toString());
    //        Assert.assertEquals(sdf.format(dateResult), "2001/12/07 00:00:00.000", sdf.format(dateResult));
    //
    //        translatedFromCycleFileFinder.setName("STOP_DATE");
    //        resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Date stopDateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sstopdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("STOP_DATE : " + sstopdf.format(stopDateResult).toString());
    //        Assert.assertEquals(sstopdf.format(stopDateResult), "2002/01/15 05:10:51.315", sstopdf.format(stopDateResult));
    //
    //    }
    //
    //    // DM60 Gestion de plusieurs fichiers ORF
    //    // Cycle inclu dans le premier fichier
    //    // FIXME TEST @Test
    //    public void test_jasonGetDateFromCycle_firstFile() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(jasonInitConfProperties1());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Integer> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_CYCLE);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("DATE");
    //        translatedFromCycleFileFinder.setName("START_DATE");
    //
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Date dateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("START_DATE : " + sdf.format(dateResult).toString());
    //        Assert.assertEquals(sdf.format(dateResult), "2002/02/04 01:07:52.802", sdf.format(dateResult));
    //
    //        translatedFromCycleFileFinder.setName("STOP_DATE");
    //        resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Date stopDateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sstopdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("STOP_DATE : " + sstopdf.format(stopDateResult).toString());
    //        Assert.assertEquals(sstopdf.format(stopDateResult), "2002/02/13 22:38:17.950", sstopdf.format(stopDateResult));
    //    }
    //
    //    // DM60 Gestion de plusieurs fichiers ORF
    //    // Cycle inclu dans le deuxieme fichier
    //    @Test
    //    public void test_jasonGetDateFromCycle_secondFile() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(jasonInitConfProperties1());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Integer> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_CYCLE_FIRST_FILE);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("DATE");
    //        translatedFromCycleFileFinder.setName("START_DATE");
    //
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Date dateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("START_DATE : " + sdf.format(dateResult).toString());
    //        Assert.assertEquals(sdf.format(dateResult), "2009/02/10 05:53:05.733", sdf.format(dateResult));
    //
    //        translatedFromCycleFileFinder.setName("STOP_DATE");
    //        resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Date stopDateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sstopdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("STOP_DATE : " + sstopdf.format(stopDateResult).toString());
    //        Assert.assertEquals(sstopdf.format(stopDateResult), "2009/02/20 03:22:50.545", sstopdf.format(stopDateResult));
    //    }
    //
    //    // DM60 Gestion de plusieurs fichiers ORF
    //    // Cycle inclu dans aucun des deux fichiers
    //    // FIXME TEST @Test
    //    public void test_jasonGetDateFromUnknownCycle() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(jasonInitConfProperties1());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Integer> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_CYCLE_UNKNOWN);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("DATE");
    //        translatedFromCycleFileFinder.setName("START_DATE");
    //
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Date dateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("START_DATE : " + sdf.format(dateResult).toString());
    //
    //        translatedFromCycleFileFinder.setName("STOP_DATE");
    //        resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Date stopDateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sstopdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("STOP_DATE : " + sstopdf.format(stopDateResult).toString());
    //
    //    }
    //
    //    // FIXME TEST @Test
    //    public void test_jasonGetCycleFromDate_equal_cycle_startDate() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(jasonInitConfProperties1());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        translatedFromCycleFileFinder.setValueType("INTEGER");
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Date> attValueList = new ArrayList<>();
    //
    //        GregorianCalendar calendar = new GregorianCalendar(2008, 7, 10); // = 2008,08,10
    //        calendar.set(Calendar.HOUR_OF_DAY, 0);
    //        calendar.set(Calendar.MINUTE, 31);
    //        calendar.set(Calendar.AM_PM, GregorianCalendar.AM);
    //
    //        attValueList.add(calendar.getTime());
    //        attributMap.put(ATT_NAME, attValueList);
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        System.out.println("CYCLE : " + resultList.get(0).toString());
    //        Assert.assertEquals(1, resultList.size());
    //        Assert.assertEquals(242, ((Integer) resultList.get(0)).intValue());
    //    }

    @Test
    public void testResources() {
        Assert.assertTrue(new File(JA2_CYCLE_FILEPATH).getAbsolutePath(), new File(JA2_CYCLE_FILEPATH).exists());
        File dir = new File(new File(JA2_ORF_FILEPATH).getParent());
        String filePattern = new File(JA2_ORF_FILEPATH).getName();
        FileFilter fileFilter = new WildcardFileFilter(filePattern);
        File[] files = dir.listFiles(fileFilter);
        Assert.assertTrue(new File(JA2_ORF_FILEPATH).getAbsolutePath(), files.length > 0);
    }

    //    // FIXME TEST @Test
    //    public void test_getCycleFromDate_over_cycle_interval() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(initConfProperties());
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Date> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_DATE_OVER);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("INTEGER");
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Assert.assertEquals(17, ((Integer) resultList.get(0)).intValue());
    //    }
    //
    //    // FIXME TEST @Test
    //    public void test_getCycleFromDate_equal_cycle_startDate() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(initConfProperties());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        translatedFromCycleFileFinder.setValueType("INTEGER");
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Date> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_DATE_EQUAL);
    //        attributMap.put(ATT_NAME, attValueList);
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Assert.assertEquals(9, ((Integer) resultList.get(0)).intValue());
    //
    //        Calendar calendar = Calendar.getInstance();
    //        calendar.setTime(ATT_VALUE_DATE_EQUAL);
    //        calendar.set(Calendar.HOUR_OF_DAY, 8);
    //        attValueList = new ArrayList<>();
    //        attValueList.add(calendar.getTime());
    //        attributMap.put(ATT_NAME, attValueList);
    //        resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Assert.assertEquals(10, ((Integer) resultList.get(0)).intValue());
    //    }
    //
    //    // FIXME TEST @Test
    //    public void test_getCycleFromDate_inside_cycle_interval() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(initConfProperties());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Date> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_DATE_INSIDE);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("INTEGER");
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Assert.assertEquals(10, ((Integer) resultList.get(0)).intValue());
    //    }
    //
    //    // FIXME TEST @Test
    //    public void test_getDateFromCycle() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(initConfProperties());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Integer> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_CYCLE);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("DATE");
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Date dateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        Assert.assertEquals(sdf.format(dateResult), "2008/07/31 21:17:08.430", sdf.format(dateResult));
    //    }
    //
    //    // FIXME TEST @Test
    //    public void test_getDateFromCycle_incorrect() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(initConfProperties());
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Integer> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_CYCLE_OVER);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("DATE_TIME");
    //
    //            translatedFromCycleFileFinder.getValueList(null, attributMap);
    //            Assert.fail("must throw exception");
    //
    //    }

    //    // DM60 Gestion de plusieurs fichiers ORF
    //    // Cycle inclu dans le fichier orf cree
    //    // FIXME TEST @Test
    //    public void test_jasonGetDateFromCycle_000() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(jasonInitConfProperties1());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Integer> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_CYCLE_000_FILE);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("DATE");
    //        translatedFromCycleFileFinder.setName("START_DATE");
    //
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Date dateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("START_DATE : " + sdf.format(dateResult).toString());
    //        Assert.assertEquals(sdf.format(dateResult), "2001/12/07 00:00:00.000", sdf.format(dateResult));
    //
    //        translatedFromCycleFileFinder.setName("STOP_DATE");
    //        resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Date stopDateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sstopdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("STOP_DATE : " + sstopdf.format(stopDateResult).toString());
    //        Assert.assertEquals(sstopdf.format(stopDateResult), "2002/01/15 05:10:51.315", sstopdf.format(stopDateResult));
    //
    //    }
    //
    //    // DM60 Gestion de plusieurs fichiers ORF
    //    // Cycle inclu dans le premier fichier
    //    // FIXME TEST @Test
    //    public void test_jasonGetDateFromCycle_firstFile() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(jasonInitConfProperties1());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Integer> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_CYCLE);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("DATE");
    //        translatedFromCycleFileFinder.setName("START_DATE");
    //
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Date dateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("START_DATE : " + sdf.format(dateResult).toString());
    //        Assert.assertEquals(sdf.format(dateResult), "2002/02/04 01:07:52.802", sdf.format(dateResult));
    //
    //        translatedFromCycleFileFinder.setName("STOP_DATE");
    //        resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Date stopDateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sstopdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("STOP_DATE : " + sstopdf.format(stopDateResult).toString());
    //        Assert.assertEquals(sstopdf.format(stopDateResult), "2002/02/13 22:38:17.950", sstopdf.format(stopDateResult));
    //    }
    //
    //    // DM60 Gestion de plusieurs fichiers ORF
    //    // Cycle inclu dans le deuxieme fichier
    //    @Test
    //    public void test_jasonGetDateFromCycle_secondFile() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(jasonInitConfProperties1());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Integer> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_CYCLE_FIRST_FILE);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("DATE");
    //        translatedFromCycleFileFinder.setName("START_DATE");
    //
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Date dateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("START_DATE : " + sdf.format(dateResult).toString());
    //        Assert.assertEquals(sdf.format(dateResult), "2009/02/10 05:53:05.733", sdf.format(dateResult));
    //
    //        translatedFromCycleFileFinder.setName("STOP_DATE");
    //        resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Date stopDateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sstopdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("STOP_DATE : " + sstopdf.format(stopDateResult).toString());
    //        Assert.assertEquals(sstopdf.format(stopDateResult), "2009/02/20 03:22:50.545", sstopdf.format(stopDateResult));
    //    }
    //
    //    // DM60 Gestion de plusieurs fichiers ORF
    //    // Cycle inclu dans aucun des deux fichiers
    //    // FIXME TEST @Test
    //    public void test_jasonGetDateFromUnknownCycle() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(jasonInitConfProperties1());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Integer> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_CYCLE_UNKNOWN);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("DATE");
    //        translatedFromCycleFileFinder.setName("START_DATE");
    //
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Date dateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("START_DATE : " + sdf.format(dateResult).toString());
    //
    //        translatedFromCycleFileFinder.setName("STOP_DATE");
    //        resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Date stopDateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sstopdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("STOP_DATE : " + sstopdf.format(stopDateResult).toString());
    //
    //    }
    //
    //    // FIXME TEST @Test
    //    public void test_jasonGetCycleFromDate_equal_cycle_startDate() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(jasonInitConfProperties1());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        translatedFromCycleFileFinder.setValueType("INTEGER");
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Date> attValueList = new ArrayList<>();
    //
    //        GregorianCalendar calendar = new GregorianCalendar(2008, 7, 10); // = 2008,08,10
    //        calendar.set(Calendar.HOUR_OF_DAY, 0);
    //        calendar.set(Calendar.MINUTE, 31);
    //        calendar.set(Calendar.AM_PM, GregorianCalendar.AM);
    //
    //        attValueList.add(calendar.getTime());
    //        attributMap.put(ATT_NAME, attValueList);
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        System.out.println("CYCLE : " + resultList.get(0).toString());
    //        Assert.assertEquals(1, resultList.size());
    //        Assert.assertEquals(242, ((Integer) resultList.get(0)).intValue());
    //    }

    //    // FIXME TEST @Test
    //    public void test_jasonGetCycleFromDate_cycle_000_startDate() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(jasonInitConfProperties1());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        translatedFromCycleFileFinder.setValueType("INTEGER");
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Date> attValueList = new ArrayList<>();
    //
    //        GregorianCalendar calendar = new GregorianCalendar(2002, 0, 14); // = 2002/01/14 59:59:59.999
    //        calendar.set(Calendar.HOUR_OF_DAY, 5);
    //        calendar.set(Calendar.MINUTE, 9);
    //        calendar.set(Calendar.AM_PM, GregorianCalendar.PM);
    //
    //        attValueList.add(calendar.getTime());
    //        attributMap.put(ATT_NAME, attValueList);
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        System.out.println("CYCLE : " + resultList.get(0).toString());
    //        Assert.assertEquals(1, resultList.size());
    //        Assert.assertEquals(000, ((Integer) resultList.get(0)).intValue());
    //    }

    //    // FIXME TEST @Test
    //    public void test_getCycleFromDate_over_cycle_interval() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(initConfProperties());
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Date> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_DATE_OVER);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("INTEGER");
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Assert.assertEquals(17, ((Integer) resultList.get(0)).intValue());
    //    }
    //
    //    // FIXME TEST @Test
    //    public void test_getCycleFromDate_equal_cycle_startDate() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(initConfProperties());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        translatedFromCycleFileFinder.setValueType("INTEGER");
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Date> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_DATE_EQUAL);
    //        attributMap.put(ATT_NAME, attValueList);
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Assert.assertEquals(9, ((Integer) resultList.get(0)).intValue());
    //
    //        Calendar calendar = Calendar.getInstance();
    //        calendar.setTime(ATT_VALUE_DATE_EQUAL);
    //        calendar.set(Calendar.HOUR_OF_DAY, 8);
    //        attValueList = new ArrayList<>();
    //        attValueList.add(calendar.getTime());
    //        attributMap.put(ATT_NAME, attValueList);
    //        resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Assert.assertEquals(10, ((Integer) resultList.get(0)).intValue());
    //    }
    //
    //    // FIXME TEST @Test
    //    public void test_getCycleFromDate_inside_cycle_interval() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(initConfProperties());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Date> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_DATE_INSIDE);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("INTEGER");
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Assert.assertEquals(10, ((Integer) resultList.get(0)).intValue());
    //    }
    //
    //    // FIXME TEST @Test
    //    public void test_getDateFromCycle() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(initConfProperties());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Integer> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_CYCLE);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("DATE");
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Date dateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        Assert.assertEquals(sdf.format(dateResult), "2008/07/31 21:17:08.430", sdf.format(dateResult));
    //    }
    //
    //    // FIXME TEST @Test
    //    public void test_getDateFromCycle_incorrect() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(initConfProperties());
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Integer> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_CYCLE_OVER);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("DATE_TIME");
    //
    //            translatedFromCycleFileFinder.getValueList(null, attributMap);
    //            Assert.fail("must throw exception");
    //
    //    }

    //    // FIXME TEST @Test
    //    public void test_getCycleFromDate_over_cycle_interval() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(initConfProperties());
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Date> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_DATE_OVER);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("INTEGER");
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Assert.assertEquals(17, ((Integer) resultList.get(0)).intValue());
    //    }
    //
    //    // FIXME TEST @Test
    //    public void test_getCycleFromDate_equal_cycle_startDate() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(initConfProperties());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        translatedFromCycleFileFinder.setValueType("INTEGER");
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Date> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_DATE_EQUAL);
    //        attributMap.put(ATT_NAME, attValueList);
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Assert.assertEquals(9, ((Integer) resultList.get(0)).intValue());
    //
    //        Calendar calendar = Calendar.getInstance();
    //        calendar.setTime(ATT_VALUE_DATE_EQUAL);
    //        calendar.set(Calendar.HOUR_OF_DAY, 8);
    //        attValueList = new ArrayList<>();
    //        attValueList.add(calendar.getTime());
    //        attributMap.put(ATT_NAME, attValueList);
    //        resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Assert.assertEquals(10, ((Integer) resultList.get(0)).intValue());
    //    }
    //
    //    // FIXME TEST @Test
    //    public void test_getCycleFromDate_inside_cycle_interval() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(initConfProperties());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Date> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_DATE_INSIDE);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("INTEGER");
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Assert.assertEquals(10, ((Integer) resultList.get(0)).intValue());
    //    }
    //
    //    // FIXME TEST @Test
    //    public void test_getDateFromCycle() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(initConfProperties());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Integer> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_CYCLE);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("DATE");
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Date dateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        Assert.assertEquals(sdf.format(dateResult), "2008/07/31 21:17:08.430", sdf.format(dateResult));
    //    }
    //
    //    // FIXME TEST @Test
    //    public void test_getDateFromCycle_incorrect() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(initConfProperties());
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Integer> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_CYCLE_OVER);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("DATE_TIME");
    //
    //            translatedFromCycleFileFinder.getValueList(null, attributMap);
    //            Assert.fail("must throw exception");
    //
    //    }

    //    // DM60 Gestion de plusieurs fichiers ORF
    //    // Cycle inclu dans le fichier orf cree
    //    // FIXME TEST @Test
    //    public void test_jasonGetDateFromCycle_000() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(jasonInitConfProperties1());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Integer> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_CYCLE_000_FILE);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("DATE");
    //        translatedFromCycleFileFinder.setName("START_DATE");
    //
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Date dateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("START_DATE : " + sdf.format(dateResult).toString());
    //        Assert.assertEquals(sdf.format(dateResult), "2001/12/07 00:00:00.000", sdf.format(dateResult));
    //
    //        translatedFromCycleFileFinder.setName("STOP_DATE");
    //        resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Date stopDateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sstopdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("STOP_DATE : " + sstopdf.format(stopDateResult).toString());
    //        Assert.assertEquals(sstopdf.format(stopDateResult), "2002/01/15 05:10:51.315", sstopdf.format(stopDateResult));
    //
    //    }
    //
    //    // DM60 Gestion de plusieurs fichiers ORF
    //    // Cycle inclu dans le premier fichier
    //    // FIXME TEST @Test
    //    public void test_jasonGetDateFromCycle_firstFile() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(jasonInitConfProperties1());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Integer> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_CYCLE);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("DATE");
    //        translatedFromCycleFileFinder.setName("START_DATE");
    //
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Date dateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("START_DATE : " + sdf.format(dateResult).toString());
    //        Assert.assertEquals(sdf.format(dateResult), "2002/02/04 01:07:52.802", sdf.format(dateResult));
    //
    //        translatedFromCycleFileFinder.setName("STOP_DATE");
    //        resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Date stopDateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sstopdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("STOP_DATE : " + sstopdf.format(stopDateResult).toString());
    //        Assert.assertEquals(sstopdf.format(stopDateResult), "2002/02/13 22:38:17.950", sstopdf.format(stopDateResult));
    //    }
    //
    //    // DM60 Gestion de plusieurs fichiers ORF
    //    // Cycle inclu dans le deuxieme fichier
    //    @Test
    //    public void test_jasonGetDateFromCycle_secondFile() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(jasonInitConfProperties1());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Integer> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_CYCLE_FIRST_FILE);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("DATE");
    //        translatedFromCycleFileFinder.setName("START_DATE");
    //
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Date dateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("START_DATE : " + sdf.format(dateResult).toString());
    //        Assert.assertEquals(sdf.format(dateResult), "2009/02/10 05:53:05.733", sdf.format(dateResult));
    //
    //        translatedFromCycleFileFinder.setName("STOP_DATE");
    //        resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Date stopDateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sstopdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("STOP_DATE : " + sstopdf.format(stopDateResult).toString());
    //        Assert.assertEquals(sstopdf.format(stopDateResult), "2009/02/20 03:22:50.545", sstopdf.format(stopDateResult));
    //    }
    //
    //    // DM60 Gestion de plusieurs fichiers ORF
    //    // Cycle inclu dans aucun des deux fichiers
    //    // FIXME TEST @Test
    //    public void test_jasonGetDateFromUnknownCycle() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(jasonInitConfProperties1());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Integer> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_CYCLE_UNKNOWN);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("DATE");
    //        translatedFromCycleFileFinder.setName("START_DATE");
    //
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Date dateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("START_DATE : " + sdf.format(dateResult).toString());
    //
    //        translatedFromCycleFileFinder.setName("STOP_DATE");
    //        resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Date stopDateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sstopdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("STOP_DATE : " + sstopdf.format(stopDateResult).toString());
    //
    //    }
    //
    //    // FIXME TEST @Test
    //    public void test_jasonGetCycleFromDate_equal_cycle_startDate() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(jasonInitConfProperties1());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        translatedFromCycleFileFinder.setValueType("INTEGER");
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Date> attValueList = new ArrayList<>();
    //
    //        GregorianCalendar calendar = new GregorianCalendar(2008, 7, 10); // = 2008,08,10
    //        calendar.set(Calendar.HOUR_OF_DAY, 0);
    //        calendar.set(Calendar.MINUTE, 31);
    //        calendar.set(Calendar.AM_PM, GregorianCalendar.AM);
    //
    //        attValueList.add(calendar.getTime());
    //        attributMap.put(ATT_NAME, attValueList);
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        System.out.println("CYCLE : " + resultList.get(0).toString());
    //        Assert.assertEquals(1, resultList.size());
    //        Assert.assertEquals(242, ((Integer) resultList.get(0)).intValue());
    //    }

    //    // FIXME TEST @Test
    //    public void test_getCycleFromDate_over_cycle_interval() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(initConfProperties());
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Date> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_DATE_OVER);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("INTEGER");
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Assert.assertEquals(17, ((Integer) resultList.get(0)).intValue());
    //    }
    //
    //    // FIXME TEST @Test
    //    public void test_getCycleFromDate_equal_cycle_startDate() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(initConfProperties());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        translatedFromCycleFileFinder.setValueType("INTEGER");
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Date> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_DATE_EQUAL);
    //        attributMap.put(ATT_NAME, attValueList);
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Assert.assertEquals(9, ((Integer) resultList.get(0)).intValue());
    //
    //        Calendar calendar = Calendar.getInstance();
    //        calendar.setTime(ATT_VALUE_DATE_EQUAL);
    //        calendar.set(Calendar.HOUR_OF_DAY, 8);
    //        attValueList = new ArrayList<>();
    //        attValueList.add(calendar.getTime());
    //        attributMap.put(ATT_NAME, attValueList);
    //        resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Assert.assertEquals(10, ((Integer) resultList.get(0)).intValue());
    //    }
    //
    //    // FIXME TEST @Test
    //    public void test_getCycleFromDate_inside_cycle_interval() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(initConfProperties());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Date> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_DATE_INSIDE);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("INTEGER");
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Assert.assertEquals(10, ((Integer) resultList.get(0)).intValue());
    //    }
    //
    //    // FIXME TEST @Test
    //    public void test_getDateFromCycle() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(initConfProperties());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Integer> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_CYCLE);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("DATE");
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Date dateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        Assert.assertEquals(sdf.format(dateResult), "2008/07/31 21:17:08.430", sdf.format(dateResult));
    //    }
    //
    //    // FIXME TEST @Test
    //    public void test_getDateFromCycle_incorrect() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(initConfProperties());
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Integer> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_CYCLE_OVER);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("DATE_TIME");
    //
    //            translatedFromCycleFileFinder.getValueList(null, attributMap);
    //            Assert.fail("must throw exception");
    //
    //    }

    //    // DM60 Gestion de plusieurs fichiers ORF
    //    // Cycle inclu dans le fichier orf cree
    //    // FIXME TEST @Test
    //    public void test_jasonGetDateFromCycle_000() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(jasonInitConfProperties1());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Integer> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_CYCLE_000_FILE);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("DATE");
    //        translatedFromCycleFileFinder.setName("START_DATE");
    //
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Date dateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("START_DATE : " + sdf.format(dateResult).toString());
    //        Assert.assertEquals(sdf.format(dateResult), "2001/12/07 00:00:00.000", sdf.format(dateResult));
    //
    //        translatedFromCycleFileFinder.setName("STOP_DATE");
    //        resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Date stopDateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sstopdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("STOP_DATE : " + sstopdf.format(stopDateResult).toString());
    //        Assert.assertEquals(sstopdf.format(stopDateResult), "2002/01/15 05:10:51.315", sstopdf.format(stopDateResult));
    //
    //    }
    //
    //    // DM60 Gestion de plusieurs fichiers ORF
    //    // Cycle inclu dans le premier fichier
    //    // FIXME TEST @Test
    //    public void test_jasonGetDateFromCycle_firstFile() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(jasonInitConfProperties1());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Integer> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_CYCLE);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("DATE");
    //        translatedFromCycleFileFinder.setName("START_DATE");
    //
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Date dateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("START_DATE : " + sdf.format(dateResult).toString());
    //        Assert.assertEquals(sdf.format(dateResult), "2002/02/04 01:07:52.802", sdf.format(dateResult));
    //
    //        translatedFromCycleFileFinder.setName("STOP_DATE");
    //        resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Date stopDateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sstopdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("STOP_DATE : " + sstopdf.format(stopDateResult).toString());
    //        Assert.assertEquals(sstopdf.format(stopDateResult), "2002/02/13 22:38:17.950", sstopdf.format(stopDateResult));
    //    }
    //
    //    // DM60 Gestion de plusieurs fichiers ORF
    //    // Cycle inclu dans le deuxieme fichier
    //    @Test
    //    public void test_jasonGetDateFromCycle_secondFile() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(jasonInitConfProperties1());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Integer> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_CYCLE_FIRST_FILE);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("DATE");
    //        translatedFromCycleFileFinder.setName("START_DATE");
    //
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Date dateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("START_DATE : " + sdf.format(dateResult).toString());
    //        Assert.assertEquals(sdf.format(dateResult), "2009/02/10 05:53:05.733", sdf.format(dateResult));
    //
    //        translatedFromCycleFileFinder.setName("STOP_DATE");
    //        resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Date stopDateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sstopdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("STOP_DATE : " + sstopdf.format(stopDateResult).toString());
    //        Assert.assertEquals(sstopdf.format(stopDateResult), "2009/02/20 03:22:50.545", sstopdf.format(stopDateResult));
    //    }
    //
    //    // DM60 Gestion de plusieurs fichiers ORF
    //    // Cycle inclu dans aucun des deux fichiers
    //    // FIXME TEST @Test
    //    public void test_jasonGetDateFromUnknownCycle() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(jasonInitConfProperties1());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Integer> attValueList = new ArrayList<>();
    //        attValueList.add(ATT_VALUE_CYCLE_UNKNOWN);
    //        attributMap.put(ATT_NAME, attValueList);
    //        translatedFromCycleFileFinder.setValueType("DATE");
    //        translatedFromCycleFileFinder.setName("START_DATE");
    //
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Assert.assertEquals(1, resultList.size());
    //        Date dateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("START_DATE : " + sdf.format(dateResult).toString());
    //
    //        translatedFromCycleFileFinder.setName("STOP_DATE");
    //        resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        Date stopDateResult = (Date) resultList.get(0);
    //        SimpleDateFormat sstopdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    //        System.out.println("STOP_DATE : " + sstopdf.format(stopDateResult).toString());
    //
    //    }
    //
    //    // FIXME TEST @Test
    //    public void test_jasonGetCycleFromDate_equal_cycle_startDate() throws Exception {
    //        TranslatedFromCycleFileFinder translatedFromCycleFileFinder = new TranslatedFromCycleFileFinder();
    //        translatedFromCycleFileFinder.setAttributProperties(jasonInitConfProperties1());
    //
    //        translatedFromCycleFileFinder.setOtherAttributeName(ATT_NAME);
    //        translatedFromCycleFileFinder.setValueType("INTEGER");
    //        Map<String, List<? extends Object>> attributMap = new HashMap<>();
    //        List<Date> attValueList = new ArrayList<>();
    //
    //        GregorianCalendar calendar = new GregorianCalendar(2008, 7, 10); // = 2008,08,10
    //        calendar.set(Calendar.HOUR_OF_DAY, 0);
    //        calendar.set(Calendar.MINUTE, 31);
    //        calendar.set(Calendar.AM_PM, GregorianCalendar.AM);
    //
    //        attValueList.add(calendar.getTime());
    //        attributMap.put(ATT_NAME, attValueList);
    //        List<Object> resultList = translatedFromCycleFileFinder.getValueList(null, attributMap);
    //        System.out.println("CYCLE : " + resultList.get(0).toString());
    //        Assert.assertEquals(1, resultList.size());
    //        Assert.assertEquals(242, ((Integer) resultList.get(0)).intValue());
    //    }

    private PluginConfigurationProperties jason2InitConfProperties() {
        PluginConfigurationProperties_mock mockProperties = new PluginConfigurationProperties_mock();
        mockProperties.setOrfFilepath(JA2_ORF_FILEPATH);
        mockProperties.setCycleFileFilepath(JA2_CYCLE_FILEPATH);
        mockProperties.setProject("JASON2");
        return mockProperties;
    }

    // DM60 Prise en compte de plusieurs fichiers ORF
    private PluginConfigurationProperties jasonInitConfProperties() {
        PluginConfigurationProperties_mock mockProperties = new PluginConfigurationProperties_mock();
        mockProperties.setProject("JASON");
        return mockProperties;
    }

}
