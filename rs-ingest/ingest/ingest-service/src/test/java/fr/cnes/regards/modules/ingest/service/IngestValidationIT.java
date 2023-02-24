/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

package fr.cnes.regards.modules.ingest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.cnes.regards.modules.model.dto.properties.adapter.IntervalMapping;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.validation.Errors;

import java.util.*;

/**
 * @author Thibaud Michaudel
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=ingest",
                                   "eureka.client.enabled=false",
                                   "regards.ingest.aip.delete.bulk.delay=100" },
                    locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = { "noscheduler" })
public class IngestValidationIT extends AbstractValidationIngestMultitenantServiceIT {

    @Autowired
    private IngestValidationService validationService;

    @Test
    public void testPrimitiveTypeValidation() {
        String modelName = mockModelClient("ingest_model_01.xml",
                                           this.getCps(),
                                           this.getFactory(),
                                           this.getDefaultTenant(),
                                           this.getModelAttrAssocClientMock());

        // Missing 5 required properties
        Errors errors = validationService.validate(modelName, Collections.emptyMap(), "Ingest1");
        if (errors.hasErrors()) {
            Assert.assertEquals(5, errors.getErrorCount());
        } else {
            Assert.fail();
        }

        Map<String, Object> descInfoOK = new HashMap<>();
        descInfoOK.put("string_field", "String value");
        descInfoOK.put("integer_field", 8);
        descInfoOK.put("double_field", 10.58);
        descInfoOK.put("long_field", 154785478L);
        descInfoOK.put("boolean_field", true);

        // No error
        errors = validationService.validate(modelName, descInfoOK, "Ingest1");
        if (errors.hasErrors()) {
            Assert.fail();
        }

        Map<String, Object> descInfoNOK = new HashMap<>();
        descInfoNOK.put("string_field", 1);
        descInfoNOK.put("integer_field", 8.9);
        descInfoNOK.put("double_field", false);
        descInfoNOK.put("long_field", "154785478");
        descInfoNOK.put("boolean_field", "true");

        // 5 properties have the wrong type
        errors = validationService.validate(modelName, descInfoNOK, "Ingest1");
        if (errors.hasErrors()) {
            Assert.assertEquals(5, errors.getErrorCount());
        } else {
            Assert.fail();
        }

        Map<String, Object> descInfoTooMany = new HashMap<>();
        descInfoTooMany.put("string_field", "String value");
        descInfoTooMany.put("integer_field", 8);
        descInfoTooMany.put("double_field", 10.58);
        descInfoTooMany.put("long_field", 154785478L);
        descInfoTooMany.put("boolean_field", true);
        descInfoTooMany.put("unexpectedField1", 1);
        descInfoTooMany.put("unexpectedField2", "value");

        // 2 unexpected properties
        errors = validationService.validate(modelName, descInfoTooMany, "Ingest1");
        if (errors.hasErrors()) {
            Assert.assertEquals(2, errors.getErrorCount());
        } else {
            Assert.fail();
        }

    }

    @Test
    public void testComplexTypeValidation() {
        String modelName = mockModelClient("ingest_model_02.xml",
                                           this.getCps(),
                                           this.getFactory(),
                                           this.getDefaultTenant(),
                                           this.getModelAttrAssocClientMock());

        Map<String, Object> descInfoOK = new HashMap<>();
        descInfoOK.put("url_field", "https://www.google.com");
        descInfoOK.put("date_field", "2022-01-01T00:00:00.000Z");

        // No error
        Errors errors = validationService.validate(modelName, descInfoOK, "Ingest2");
        if (errors.hasErrors()) {
            Assert.fail();
        }

        Map<String, Object> descInfoWrongType = new HashMap<>();
        descInfoWrongType.put("url_field", 5);
        descInfoWrongType.put("date_field", true);

        // 2 properties have the wrong type
        errors = validationService.validate(modelName, descInfoWrongType, "Ingest2");
        if (errors.hasErrors()) {
            Assert.assertEquals(2, errors.getErrorCount());
        } else {
            Assert.fail();
        }

        Map<String, Object> descInfoWrongFormat = new HashMap<>();
        descInfoWrongFormat.put("url_field", "google.com");
        descInfoWrongFormat.put("date_field", "2022-01-01");

        // 2 properties have the wrong format
        errors = validationService.validate(modelName, descInfoWrongFormat, "Ingest2");
        if (errors.hasErrors()) {
            Assert.assertEquals(2, errors.getErrorCount());
        } else {
            Assert.fail();
        }
    }

    @Test
    public void testArrayTypeValidation() {
        String modelName = mockModelClient("ingest_model_03.xml",
                                           this.getCps(),
                                           this.getFactory(),
                                           this.getDefaultTenant(),
                                           this.getModelAttrAssocClientMock());

        Map<String, Object> descInfoOK = new HashMap<>();
        descInfoOK.put("string_array_field", new ArrayList<String>());
        descInfoOK.put("integer_array_field", Arrays.asList(1));
        descInfoOK.put("double_array_field", Arrays.asList(2.2, 3.3));
        descInfoOK.put("long_array_field", Arrays.asList(444L, 555L, 666L));
        descInfoOK.put("date_array_field", Arrays.asList("2022-01-01T00:00:00.000Z", "2022-01-02T00:00:00.000"));

        // No error
        Errors errors = validationService.validate(modelName, descInfoOK, "Ingest3");
        if (errors.hasErrors()) {
            Assert.fail();
        }

        Map<String, Object> descInfoNOK = new HashMap<>();
        descInfoNOK.put("string_array_field", new String[] { "Value1", "Value2" });
        descInfoNOK.put("integer_array_field", Arrays.asList(1, 2, 3, 4.5));
        descInfoNOK.put("double_array_field", 4.5);
        descInfoNOK.put("long_array_field", Arrays.asList(2.2, 3.3));
        descInfoNOK.put("date_array_field", Arrays.asList("Value1", "Value2"));

        // 5 properties have the wrong type
        errors = validationService.validate(modelName, descInfoNOK, "Ingest3");
        if (errors.hasErrors()) {
            Assert.assertEquals(5, errors.getErrorCount());
        } else {
            Assert.fail();
        }

    }

    @Test
    public void testIntervalTypeValidation() {
        String modelName = mockModelClient("ingest_model_04.xml",
                                           this.getCps(),
                                           this.getFactory(),
                                           this.getDefaultTenant(),
                                           this.getModelAttrAssocClientMock());

        Map<String, Object> descInfoOK = new HashMap<>();
        descInfoOK.put("integer_interval_field", buildIntervalMap(10, 20));
        descInfoOK.put("double_interval_field", buildIntervalMap(2.2, 3.3));
        descInfoOK.put("long_interval_field", buildIntervalMap(444L, 555L));
        descInfoOK.put("date_interval_field", buildIntervalMap("2022-01-01T00:00:00.000Z", "2022-01-02T00:00:00.000"));

        // No error
        Errors errors = validationService.validate(modelName, descInfoOK, "Ingest4");
        if (errors.hasErrors()) {
            Assert.fail();
        }

        Map<String, Object> descInfoNOK = new HashMap<>();

        HashMap<String, Object> badIntervalMap = new HashMap<>();
        badIntervalMap.put(IntervalMapping.RANGE_LOWER_BOUND, 10);
        badIntervalMap.put("upper_bound", 20);
        descInfoNOK.put("integer_interval_field", badIntervalMap);
        descInfoNOK.put("double_interval_field", Arrays.asList(2.2, 3.3));
        descInfoNOK.put("long_interval_field", buildIntervalMap(444L, 555.55));
        descInfoNOK.put("date_interval_field", buildIntervalMap("2022-01-01", "2022-01-02T00:00:00.000"));

        // 4 properties have the wrong type
        errors = validationService.validate(modelName, descInfoNOK, "Ingest4");
        if (errors.hasErrors()) {
            Assert.assertEquals(4, errors.getErrorCount());
        } else {
            Assert.fail();
        }

    }

    @Test
    public void testJsonTypeValidation() throws JsonProcessingException {
        String modelName = mockModelClient("ingest_model_05.xml",
                                           this.getCps(),
                                           this.getFactory(),
                                           this.getDefaultTenant(),
                                           this.getModelAttrAssocClientMock());

        Map<String, Object> descInfoOK = new HashMap<>();
        String goodJson = "{\n"
                          + "    \"glossary\": {\n"
                          + "        \"title\": \"example glossary\",\n"
                          + "\t\t\"GlossDiv\": {\n"
                          + "            \"title\": \"S\",\n"
                          + "\t\t\t\"GlossList\": {\n"
                          + "                \"GlossEntry\": {\n"
                          + "                    \"ID\": \"SGML\",\n"
                          + "\t\t\t\t\t\"SortAs\": \"SGML\",\n"
                          + "\t\t\t\t\t\"GlossTerm\": \"Standard Generalized Markup Language\",\n"
                          + "\t\t\t\t\t\"Acronym\": \"SGML\",\n"
                          + "\t\t\t\t\t\"Abbrev\": \"ISO 8879:1986\",\n"
                          + "\t\t\t\t\t\"GlossDef\": {\n"
                          + "                        \"para\": \"A meta-markup language, used to create markup languages such as DocBook.\",\n"
                          + "\t\t\t\t\t\t\"GlossSeeAlso\": [\"GML\", \"XML\"]\n"
                          + "                    },\n"
                          + "\t\t\t\t\t\"GlossSee\": \"markup\"\n"
                          + "                }\n"
                          + "            }\n"
                          + "        }\n"
                          + "    }\n"
                          + "}\n";
        descInfoOK.put("json_field", new ObjectMapper().readValue(goodJson, HashMap.class));

        // No error
        Errors errors = validationService.validate(modelName, descInfoOK, "Ingest5");
        if (errors.hasErrors()) {
            Assert.fail();
        }

        Map<String, Object> descInfoNOK = new HashMap<>();
        descInfoNOK.put("json_field", Arrays.asList("This", "is", "not", "json"));

        // Not json
        errors = validationService.validate(modelName, descInfoNOK, "Ingest5");
        if (errors.hasErrors()) {
            Assert.assertEquals(1, errors.getErrorCount());
        } else {
            Assert.fail();
        }
    }

    @Test
    public void testEnumerationRestrictionValidation() {
        String modelName = mockModelClient("ingest_model_06.xml",
                                           this.getCps(),
                                           this.getFactory(),
                                           this.getDefaultTenant(),
                                           this.getModelAttrAssocClientMock());

        Map<String, Object> descInfoOK = new HashMap<>();
        descInfoOK.put("enum_field", "Toulouse");
        descInfoOK.put("enum_array_field", Arrays.asList("Toulouse", "Bordeaux"));

        // enumeration restriction respected
        Errors errors = validationService.validate(modelName, descInfoOK, "Ingest6");
        if (errors.hasErrors()) {
            Assert.fail();
        }

        Map<String, Object> descInfoNOK = new HashMap<>();
        descInfoNOK.put("enum_field", "Paris");
        descInfoNOK.put("enum_array_field", Arrays.asList("Toulouse", "Paris"));

        // 2 enumeration restrictions not respected
        errors = validationService.validate(modelName, descInfoNOK, "Ingest6");
        if (errors.hasErrors()) {
            Assert.assertEquals(2, errors.getErrorCount());
        } else {
            Assert.fail();
        }
    }

    @Test
    public void testRangeRestrictionValidation() {
        String modelName = mockModelClient("ingest_model_07.xml",
                                           this.getCps(),
                                           this.getFactory(),
                                           this.getDefaultTenant(),
                                           this.getModelAttrAssocClientMock());

        Map<String, Object> descInfoOK = new HashMap<>();
        descInfoOK.put("strict_integer_field", 6);
        descInfoOK.put("integer_field", 5);
        descInfoOK.put("long_field", 50l);
        descInfoOK.put("strict_double_field", 10.4);
        descInfoOK.put("double_field", 10.5);

        // No error
        Errors errors = validationService.validate(modelName, descInfoOK, "Ingest7");
        if (errors.hasErrors()) {
            Assert.fail();
        }

        Map<String, Object> descInfoNOK = new HashMap<>();
        descInfoNOK.put("strict_integer_field", 5);
        descInfoNOK.put("integer_field", 4);
        descInfoNOK.put("long_field", 80l);
        descInfoNOK.put("strict_double_field", 10.5);
        descInfoNOK.put("double_field", 11.0);

        // 5 properties don't respect the range restrictions
        errors = validationService.validate(modelName, descInfoNOK, "Ingest7");
        if (errors.hasErrors()) {
            Assert.assertEquals(5, errors.getErrorCount());
        } else {
            Assert.fail();
        }
    }

    @Test
    public void testPatternRestrictionValidation() {
        String modelName = mockModelClient("ingest_model_08.xml",
                                           this.getCps(),
                                           this.getFactory(),
                                           this.getDefaultTenant(),
                                           this.getModelAttrAssocClientMock());

        Map<String, Object> descInfoOK = new HashMap<>();
        descInfoOK.put("string_field", "Alexandria");
        descInfoOK.put("string_array_field", Arrays.asList("CAIRO", "JERUSALEM", "BEIRUT"));

        // No error
        Errors errors = validationService.validate(modelName, descInfoOK, "Ingest8");
        if (errors.hasErrors()) {
            Assert.fail();
        }

        Map<String, Object> descInfoNOK = new HashMap<>();
        descInfoNOK.put("string_field", "Tel Aviv");
        descInfoNOK.put("string_array_field", Arrays.asList("Amman", ""));

        // string_field properties and both the entries of string_array_field have an error
        errors = validationService.validate(modelName, descInfoNOK, "Ingest8");
        if (errors.hasErrors()) {
            Assert.assertEquals(3, errors.getErrorCount());
        } else {
            Assert.fail();
        }
    }

    @Test
    public void testJsonRestrictionValidation() throws JsonProcessingException {
        String modelName = mockModelClient("ingest_model_09.xml",
                                           this.getCps(),
                                           this.getFactory(),
                                           this.getDefaultTenant(),
                                           this.getModelAttrAssocClientMock());

        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> descInfoOK = new HashMap<>();
        descInfoOK.put("json_field",
                       mapper.readValue("{\n"
                                        + "    \"name\": \"Bob\",\n"
                                        + "    \"city\": {\n"
                                        + "        \"name\": \"Toulouse\",\n"
                                        + "        \"country\": \"France\"\n"
                                        + "    }\n"
                                        + "}", Map.class));

        // No error
        Errors errors = validationService.validate(modelName, descInfoOK, "Ingest9");
        if (errors.hasErrors()) {
            Assert.fail();
        }

        Map<String, Object> descInfoNOK = new HashMap<>();
        descInfoNOK.put("json_field",
                        mapper.readValue("{\n"
                                         + "    \"name\": \"Bob\",\n"
                                         + "    \"city\": {\n"
                                         + "        \"name\": \"Toulouse\",\n"
                                         + "        \"country\": 5,\n"
                                         + "        \"size\": \"big\"\n"
                                         + "    }\n"
                                         + "}", Map.class));

        // two errors in json against json schema
        errors = validationService.validate(modelName, descInfoNOK, "Ingest9");
        if (errors.hasErrors()) {
            Assert.assertEquals(2, errors.getErrorCount());
        } else {
            Assert.fail();
        }
    }

    @Test
    public void testFragmentValidation() {
        String modelName = mockModelClient("ingest_model_10.xml",
                                           this.getCps(),
                                           this.getFactory(),
                                           this.getDefaultTenant(),
                                           this.getModelAttrAssocClientMock());

        Map<String, Object> descInfoOK = new HashMap<>();
        Map<String, Object> fragmentOK = new HashMap<>();
        fragmentOK.put("startDate", "2022-01-01T00:00:00.000Z");
        fragmentOK.put("stopDate", "2022-01-10T00:00:00.000Z");
        descInfoOK.put("TimePeriod", fragmentOK);

        // No error
        Errors errors = validationService.validate(modelName, descInfoOK, "Ingest10");
        if (errors.hasErrors()) {
            Assert.fail();
        }

        Map<String, Object> descInfoBadAttribute = new HashMap<>();
        Map<String, Object> fragmentBadAttribute = new HashMap<>();
        fragmentBadAttribute.put("startDate", "2022-01-01T00:00:00.000Z");
        fragmentBadAttribute.put("stopDate", "2022-45-10T00:00:00.000Z");
        descInfoBadAttribute.put("TimePeriod", fragmentBadAttribute);

        // 1 error in an attribute of the fragment
        errors = validationService.validate(modelName, descInfoBadAttribute, "Ingest10");
        if (errors.hasErrors()) {
            Assert.assertEquals(1, errors.getErrorCount());
        } else {
            Assert.fail();
        }

        Map<String, Object> descInfoMissingAttribute = new HashMap<>();
        Map<String, Object> fragmentMissingAttribute = new HashMap<>();
        fragmentMissingAttribute.put("startDate", "2022-01-01T00:00:00.000Z");
        descInfoMissingAttribute.put("TimePeriod", fragmentMissingAttribute);

        // 1 missing attribute in the fragment
        errors = validationService.validate(modelName, descInfoMissingAttribute, "Ingest10");
        if (errors.hasErrors()) {
            Assert.assertEquals(1, errors.getErrorCount());
        } else {
            Assert.fail();
        }

        Map<String, Object> descInfoTooMany = new HashMap<>();
        Map<String, Object> fragmentTooMany = new HashMap<>();
        fragmentTooMany.put("startDate", "2022-01-01T00:00:00.000Z");
        fragmentTooMany.put("stopDate", "2022-01-10T00:00:00.000Z");
        fragmentTooMany.put("christmasDate", "2022-12-25T00:00:00.000Z");
        descInfoTooMany.put("TimePeriod", fragmentTooMany);

        // 1 too many attribute in the fragment
        errors = validationService.validate(modelName, descInfoTooMany, "Ingest10");
        if (errors.hasErrors()) {
            Assert.assertEquals(1, errors.getErrorCount());
        } else {
            Assert.fail();
        }

    }

    @Test
    public void testIntegerAsLongValidation() {
        String modelName = mockModelClient("ingest_model_11.xml",
                                           this.getCps(),
                                           this.getFactory(),
                                           this.getDefaultTenant(),
                                           this.getModelAttrAssocClientMock());

        Map<String, Object> descInfoOK = new HashMap<>();
        descInfoOK.put("integer_field", 8L);
        descInfoOK.put("integer_interval_field", buildIntervalMap(6L, 9L));
        descInfoOK.put("integer_array_field", Arrays.asList(6L, 7L, 8L));

        // No error
        Errors errors = validationService.validate(modelName, descInfoOK, "Ingest11");
        if (errors.hasErrors()) {
            Assert.fail();
        }

        Map<String, Object> descInfoNOK = new HashMap<>();
        descInfoNOK.put("integer_field", 2L);
        descInfoNOK.put("integer_interval_field", buildIntervalMap(2L, 9L));
        descInfoNOK.put("integer_array_field", Arrays.asList(2L, 7L, 8L));

        // 3 errors because of restrictions
        errors = validationService.validate(modelName, descInfoNOK, "Ingest11");
        if (errors.hasErrors()) {
            Assert.assertEquals(3, errors.getErrorCount());
        } else {
            Assert.fail();
        }

    }

    private static Map<String, Object> buildIntervalMap(Object lowerBound, Object upperBound) {
        HashMap<String, Object> intervalMap = new HashMap<>();
        intervalMap.put(IntervalMapping.RANGE_LOWER_BOUND, lowerBound);
        intervalMap.put(IntervalMapping.RANGE_UPPER_BOUND, upperBound);
        return intervalMap;
    }

    @Test
    public void testCast() {
        ArrayList<Integer> pTarget = new ArrayList<Integer>();
        Integer[] actual = ((List<Integer>) pTarget).toArray(new Integer[((List) pTarget).size()]);
        Integer[] expected = new Integer[0];
        Assert.assertArrayEquals(expected, actual);

        ArrayList<Integer> pTarget2 = new ArrayList<Integer>();
        pTarget2.add(5);
        pTarget2.add(8);
        pTarget2.add(9);
        Integer[] actual2 = ((List<Integer>) pTarget2).toArray(new Integer[((List) pTarget2).size()]);
        Integer[] expected2 = { 5, 8, 9 };
        Assert.assertArrayEquals(expected2, actual2);
    }
}
