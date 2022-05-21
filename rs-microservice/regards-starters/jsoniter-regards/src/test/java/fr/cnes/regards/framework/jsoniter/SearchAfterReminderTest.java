package fr.cnes.regards.framework.jsoniter;

import com.google.gson.Gson;
import com.jsoniter.JsonIterator;
import fr.cnes.regards.modules.indexer.domain.reminder.SearchAfterReminder;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import io.vavr.control.Option;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

public class SearchAfterReminderTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchAfterReminderTest.class);

    private static JsoniterDecoderRegisterer jsoniterDecoderRegisterer;

    @BeforeClass
    public static void setupJsoniter() {
        jsoniterDecoderRegisterer = new JsoniterDecoderRegisterer(name -> {
            switch (name) {
                case "FileSize":
                    return Option.of(PropertyType.LONG);
                case "Code":
                case "Name":
                default:
                    return Option.of(PropertyType.STRING);
            }
        }, new Gson());
    }

    @Test
    public void parseSearchAfterReminderTest() throws Exception {
        String content = readResource("searchafterreminder.json");
        SearchAfterReminder parsed = JsonIterator.deserialize(content, SearchAfterReminder.class);

        OffsetDateTime expirationDate = OffsetDateTime.parse("2021-06-18T12:09:50.43Z");

        Assertions.assertEquals("8dd74f5daac073480e56a633e5abd2980b8437ec", parsed.getDocId());
        Assertions.assertEquals(expirationDate, parsed.getExpirationDate());

        LOGGER.info("parsed: {}", parsed);
    }

    private String readResource(String s) throws IOException {
        InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(s);
        return IOUtils.toString(in, StandardCharsets.UTF_8);
    }

}
