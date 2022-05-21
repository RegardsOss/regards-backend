package fr.cnes.regards.framework.jsoniter;

import com.google.gson.Gson;
import com.jsoniter.JsonIterator;
import fr.cnes.regards.modules.dam.domain.entities.Collection;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import io.vavr.control.Option;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class IIndexableJsoniterParserTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(IIndexableJsoniterParserTest.class);

    private static JsoniterDecoderRegisterer jsoniterDecoderRegisterer;

    @BeforeClass
    public static void setupJsoniter() {
        jsoniterDecoderRegisterer = new JsoniterDecoderRegisterer(name -> {
            switch (name) {
                case "Code":
                    return Option.of(PropertyType.STRING);
                case "Name":
                    return Option.of(PropertyType.STRING);
                case "FileSize":
                    return Option.of(PropertyType.LONG);
                default:
                    return Option.of(PropertyType.STRING);
            }
        }, new Gson());
    }

    @Test
    public void parseDataobjectTest() throws Exception {
        String content = readResource("dataobject.json");
        LOGGER.info("Before data object parse");
        IIndexable parsed = JsonIterator.deserialize(content, IIndexable.class);
        LOGGER.info("After data object parse");
        parsed = JsonIterator.deserialize(content, IIndexable.class);
        LOGGER.info("After data object parse again");

        assertThat(parsed).isInstanceOf(DataObject.class);
        DataObject dataobj = (DataObject) parsed;

        assertThat(dataobj.getId()).isEqualTo(123456L);
        assertThat(dataobj.getFeature().getId().toString()).isEqualTo(
            "URN:AIP:DATA:perf:35a8b1aa-7d90-3f34-bc94-646424f8cee3:V1");
        assertThat(dataobj.getFeature().getProperty("FileSize").getValue()).isEqualTo(3688L);

        LOGGER.info("parsed: {}", parsed);
    }

    @Test
    public void parseCollectionTest() throws Exception {
        String content = readResource("collection.json");

        LOGGER.info("Before collection parse");
        IIndexable parsed = JsonIterator.deserialize(content, IIndexable.class);
        LOGGER.info("After collection parse");
        parsed = JsonIterator.deserialize(content, IIndexable.class);
        LOGGER.info("After collection parse again");

        assertThat(parsed).isInstanceOf(Collection.class);
        Collection coll = (Collection) parsed;

        assertThat(coll.getId()).isEqualTo(352L);
        assertThat(coll.getFeature().getId().toString()).isEqualTo(
            "URN:AIP:COLLECTION:perf:7c310022-609a-4716-b5c7-6c3321f60bc9:V1");

        LOGGER.info("parsed: {}", parsed);
    }

    @Test
    public void parseDatasetTest() throws Exception {
        String content = readResource("dataset.json");

        LOGGER.info("Before dataset parse");
        IIndexable parsed = JsonIterator.deserialize(content, IIndexable.class);
        LOGGER.info("After dataset parse");
        parsed = JsonIterator.deserialize(content, IIndexable.class);
        LOGGER.info("After dataset parse again");

        assertThat(parsed).isInstanceOf(Dataset.class);
        Dataset dataset = (Dataset) parsed;

        assertThat(dataset.getId()).isEqualTo(402L);
        assertThat(dataset.getFeature().getId().toString()).isEqualTo(
            "URN:AIP:DATASET:perf:249a033b-2b61-44a8-b903-9805c2170654:V1");

        LOGGER.info("parsed: {}", parsed);
    }

    private String readResource(String s) throws IOException {
        InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(s);
        return IOUtils.toString(in, StandardCharsets.UTF_8);
    }

}
