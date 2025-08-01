/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.framework.utils.parser;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.cnes.regards.framework.utils.parser.rule.IRule;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Test for {@link JsonObjectMatchVisitor}. This test in its current state does not fully cover the tested class, but
 * is a good bootstrap for adding more cases.
 *
 * @author Julien Canches
 */
public class JsonObjectMatchVisitorTest {

    static List<Arguments> match() {
        return List.of(
            // From a real-world case (SWOT)
            Arguments.of("properties.regards.stored:true AND properties.jpl_sds.tend:/.*/", """
                {"id":"L2_RAD_GDR:63e0e46e-5ab2-3926-a5e8-0fbec3c64d89","urn":"URN:FEATURE:DATA:seiya:9d2f0dc3-771a-3e61-9211-ae23d0afdbd3:V1","last":true,"type":"Feature","files":[{"locations":[{"url":"http://rs-minio:9000/bucket-swot/seiya/L2_RAD_GDR/2025/08/URN:FEATURE:DATA:seiya:9d2f0dc3-771a-3e61-9211-ae23d0afdbd3:V1/tar/SWOT_GPRAD_2PfP003_069_20100102_050515_20100102_221012_VI0059_01.tar.nc","storage":"Minio-Swot"}],"attributes":{"checksum":"980F0044441DDEE21F0B2F7FB3FA8B7A","dataType":"RAWDATA","filename":"SWOT_GPRAD_2PfP003_069_20100102_050515_20100102_221012_VI0059_01.tar.nc","filesize":95,"mimeType":"text/plain","algorithm":"MD5"}},{"locations":[{"url":"http://rs-minio:9000/bucket-swot/seiya/L2_RAD_GDR/2025/08/URN:FEATURE:DATA:seiya:9d2f0dc3-771a-3e61-9211-ae23d0afdbd3:V1/tar/README.txt","storage":"Minio-Swot"}],"attributes":{"checksum":"C29EA5BBF85531ACC0E870FAE244E695","dataType":"RAWDATA","filename":"README.txt","filesize":97,"mimeType":"text/plain","algorithm":"MD5"}},{"locations":[{"url":"http://rs-minio:9000/bucket-swot/seiya/L2_RAD_GDR/2025/08/URN:FEATURE:DATA:seiya:e3bd3ddd-de5b-34a2-bf3c-07d2985be95f:V1/tar/iso.xml","storage":"Minio-Swot"}],"attributes":{"checksum":"BF39BC048B52618838529D9D98043190","dataType":"RAWDATA","filename":"iso.xml","filesize":190,"mimeType":"application/xml","algorithm":"MD5"}}],"model":"SWOT","history":{"createdBy":"chronos","updatedBy":"sds"},"entityType":"DATA","properties":{"data":{"type":"L2_RAD_GDR","end_date":"2010-01-02T22:10:12Z","start_date":"2010-01-02T05:05:15Z"},"swot":{"crid":"VI0059","pass_number":69,"cycle_number":3,"granule_type":"Pass","product_counter":1,"product_version":"f"},"system":{"filename":"SWOT_GPRAD_2PfP003_069_20100102_050515_20100102_221012_VI0059_01.tar","filesize":10240,"gpfs_url":"file:/gpfs/chronos/L2_RAD_GDR/SWOT_GPRAD_2PfP003_069_20100102_050515_20100102_221012_VI0059_01.tar","extension":"tar","archive_url":"s3://bucket-swot/seiya/L2_RAD_GDR/2025/08/URN:FEATURE:DATA:seiya:9d2f0dc3-771a-3e61-9211-ae23d0afdbd3:V1/tar/SWOT_GPRAD_2PfP003_069_20100102_050515_20100102_221012_VI0059_01.tar.nc","change_date":"2025-08-05T15:47:49.927791Z","ingestion_date":"2025-08-05T15:47:49.927791Z"},"jpl_sds":{"tend":"2020-03-03T23:00:00Z"},"regards":{"stored":true,"store_date":"2025-08-05T15:47:58.001752Z","store_duration":108.0}}}"""),
            // Another real-word case (SWOT)
            Arguments.of("action:UPDATED AND changedAttributes:properties.jpl_sds.tend", """
                {"action":"UPDATED","session":"Referencement 2025-08-05 2460","sessionOwner":"chronos","changedAttributes":["properties.jpl_sds.tend"]}"""));
    }

    @ParameterizedTest
    @MethodSource
    void match(String rule, String json) throws QueryNodeException {
        RuleParser ruleParser = new RuleParser();
        IRule parsedRule = ruleParser.parse(rule, "defaultField");
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        assertThat(parsedRule.accept(new JsonObjectMatchVisitor(jsonObject))).isEqualTo(Boolean.TRUE);
    }
}
