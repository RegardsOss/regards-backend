package fr.cnes.regards.framework.jsoniter.decoders;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.JsoniterSpi;
import fr.cnes.regards.modules.indexer.domain.reminder.SearchAfterReminder;

import java.io.IOException;
import java.time.OffsetDateTime;

public class SearchAfterReminderDecoder implements NullSafeDecoderBuilder {

    public static Decoder selfRegister() {
        Decoder decoder = new SearchAfterReminderDecoder().nullSafe();
        JsoniterSpi.registerTypeDecoder(SearchAfterReminder.class, decoder);
        return decoder;
    }

    @Override
    public Object decode(JsonIterator jsonIterator) throws IOException {
        Any param = jsonIterator.readAny();
        return new SearchAfterReminder(
                param.toString("searchAfterSortValues"),
                param.toLong("nextOffset"),
                param.toInt("nextPageSize"),
                param.toString("docId"),
                param.as(OffsetDateTime.class, "expirationDate"));
    }

}
