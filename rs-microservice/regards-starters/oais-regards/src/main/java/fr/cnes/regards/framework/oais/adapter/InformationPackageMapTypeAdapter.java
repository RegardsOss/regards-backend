package fr.cnes.regards.framework.oais.adapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterBean;

/**
 * Hack Gson to deserialize long as longs and not double when no structure is given.
 * {@link InformationPackageMapTypeAdapter#read(JsonReader)} is greatly inspired from {@link com.google.gson.internal.bind.ObjectTypeAdapter}
 * @author Sylvain VISSIERE-GUERINET
 */
@GsonTypeAdapterBean(adapted = InformationPackageMap.class)
public class InformationPackageMapTypeAdapter extends TypeAdapter<InformationPackageMap>
        implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(InformationPackageMapTypeAdapter.class);

    /**
     * {@link Gson} instance
     */
    private Gson gson;

    private ApplicationContext applicationContext;

    @Override
    public void write(JsonWriter out, InformationPackageMap value) throws IOException {
        @SuppressWarnings("rawtypes")
        TypeAdapter<Map> mapAdapter = gson.getAdapter(Map.class);
        mapAdapter.write(out, value);
    }

    @Override
    public InformationPackageMap read(JsonReader in) throws IOException {
        //let start by reading the opening brace, then lets handle each element thanks to readElement
        InformationPackageMap result = new InformationPackageMap();
        in.beginObject();
        while (in.peek() != JsonToken.END_OBJECT) {
            result.put(in.nextName(), readElement(in));
        }
        in.endObject();
        return result;
    }

    private Object readElement(JsonReader in) throws IOException {
        JsonToken token = in.peek();
        switch (token) {
            case BEGIN_ARRAY:
                List<Object> list = new ArrayList<>();
                in.beginArray();
                while (in.hasNext()) {
                    list.add(readElement(in));
                }
                in.endArray();
                return list;

            case BEGIN_OBJECT:
                Map<String, Object> map = new LinkedTreeMap<>();
                in.beginObject();
                while (in.hasNext()) {
                    map.put(in.nextName(), readElement(in));
                }
                in.endObject();
                return map;

            case STRING:
                return in.nextString();

            case NUMBER:
                String valueAsString = in.nextString();
                if (valueAsString.contains(".")) {
                    return Double.parseDouble(valueAsString);
                }
                return Long.parseLong(valueAsString);

            case BOOLEAN:
                return in.nextBoolean();

            case NULL:
                in.nextNull();
                return null;

            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (this.gson == null) {
            //lets see if gson has been configured
            try {
                this.gson = applicationContext.getBean(Gson.class);
            } catch (NoSuchBeanDefinitionException e) {
                LOG.trace("Gson has not been initialized yet", e);
            }
        }
    }
}
