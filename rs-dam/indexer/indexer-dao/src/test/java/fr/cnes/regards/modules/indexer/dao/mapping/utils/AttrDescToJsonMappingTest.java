package fr.cnes.regards.modules.indexer.dao.mapping.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.cnes.regards.modules.indexer.dao.mapping.AttributeDescription;
import fr.cnes.regards.modules.model.domain.attributes.restriction.RestrictionType;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import org.junit.Test;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;

import static fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter.ISO_DATE_TIME_UTC;
import static fr.cnes.regards.modules.indexer.dao.mapping.utils.AttrDescToJsonMapping.RangeAliasStrategy.GTELTE;
import static org.assertj.core.api.Assertions.assertThat;

public class AttrDescToJsonMappingTest {

    @Test
    public void toJsonMapping() {

        System.out.println(ISO_DATE_TIME_UTC);
        System.out.println(ISO_DATE_TIME_UTC.format(OffsetDateTime.now(ZoneId.of("UTC"))));

    }

    @Test
    public void nestedPropertiesStructure() {
        String innerStr = "{\"type\":\"number\"}";
        JsonObject innermost = new JsonParser().parse(innerStr).getAsJsonObject();

        JsonObject jsonObject = AttrDescToJsonMapping.nestedPropertiesStructure("high.medium.low", innermost);

        String json = jsonObject.toString();
        assertThat(json).isEqualTo("{\"properties\":{\"high\":{\"properties\":{\"medium\":{\"properties\":{\"low\":"
                                   + innerStr
                                   + "}}}}}}");
    }

    @Test
    public void toJsonMappingDateIntervalWithAlias() {
        // GIVEN
        AttrDescToJsonMapping attrDescToJsonMapping = new AttrDescToJsonMapping(GTELTE);
        AttributeDescription attrDesc = new AttributeDescription("some.nested.prop",
                                                                 PropertyType.DATE_INTERVAL,
                                                                 RestrictionType.NO_RESTRICTION,
                                                                 new HashMap<>(),
                                                                 null);

        // WHEN
        JsonObject mapping = attrDescToJsonMapping.toJsonMapping(attrDesc);

        // THEN
        String json = mapping.toString();
        assertThat(json).isEqualTo(
            "{\"properties\":{\"some\":{\"properties\":{\"nested\":{\"properties\":{\"prop\":{\"properties\":{"
            + "\"lowerBound\":{\"type\":\"date\",\"index\":false,\"format\":\"date_optional_time\"},"
            + "\"upperBound\":{\"type\":\"date\",\"index\":false,\"format\":\"date_optional_time\"},"
            + "\"gte\":{\"type\":\"alias\",\"index\":false,\"path\":\"some.nested.prop.lowerBound\"},"
            + "\"lte\":{\"type\":\"alias\",\"index\":false,\"path\":\"some.nested.prop.upperBound\"}}}}}}}}}");
    }
}