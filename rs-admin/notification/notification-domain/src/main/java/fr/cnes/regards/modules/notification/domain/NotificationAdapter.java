package fr.cnes.regards.modules.notification.domain;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapter;

import java.io.IOException;


@GsonTypeAdapter(adapted = INotificationWithoutMessage.class)
public class NotificationAdapter extends TypeAdapter<INotificationWithoutMessage> {

    @Override
    public void write(JsonWriter out, INotificationWithoutMessage value) throws IOException {
        out.beginObject();
        out.name("date");
        out.value(value.getDate().format(OffsetDateTimeAdapter.ISO_DATE_TIME_UTC));
        out.name("id");
        out.value(value.getId());
        out.name("sender");
        out.value(value.getSender());
        out.name("status");
        out.value(value.getStatus().toString());
        out.name("level");
        out.value(value.getLevel().toString());
        out.name("title");
        out.value(value.getTitle());
        out.name("mimetype");
        out.value(value.getMimeType().toString());
        out.endObject();
    }

    @Override
    public INotificationWithoutMessage read(JsonReader in) throws IOException {
        return null;
    }
}
