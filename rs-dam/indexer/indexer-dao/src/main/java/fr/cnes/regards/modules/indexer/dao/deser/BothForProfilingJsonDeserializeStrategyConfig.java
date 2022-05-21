package fr.cnes.regards.modules.indexer.dao.deser;

import fr.cnes.regards.modules.indexer.domain.IIndexable;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class BothForProfilingJsonDeserializeStrategyConfig implements JsonDeserializeStrategy<IIndexable> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BothForProfilingJsonDeserializeStrategyConfig.class);

    private final GsonDeserializeIIndexableStrategy withGson;

    private final JsoniterDeserializeIIndexableStrategy withJsoniter;

    public BothForProfilingJsonDeserializeStrategyConfig(GsonDeserializeIIndexableStrategy withGson,
                                                         JsoniterDeserializeIIndexableStrategy withJsoniter) {
        this.withGson = withGson;
        this.withJsoniter = withJsoniter;
    }

    @Override
    public <U extends IIndexable> U deserializeJson(String sourceAsString, Class<U> clazz) {
        LOGGER.info("Deserializing {}", sourceAsString);

        U jsoniterResult = null;
        U gsonResult = null;
        StopWatch swG = new StopWatch();
        swG.start();
        swG.suspend();
        StopWatch swJ = new StopWatch();
        swJ.start();
        swJ.suspend();
        for (int i = 0; i < 500; i++) {
            //LOGGER.info("=== Before Jsoniter");
            swJ.resume();
            jsoniterResult = withJsoniter.deserializeJson(sourceAsString, clazz);
            swJ.suspend();
            //LOGGER.info("=== After Jsoniter: {}", jsoniterResult);

            //LOGGER.info("=== Before GSON");
            swG.resume();
            gsonResult = withGson.deserializeJson(sourceAsString, clazz);
            swG.suspend();
            //LOGGER.info("=== After GSON: {}", gsonResult);
        }

        LOGGER.info("===== Jsoniter: {}ms ; Gson: {}ms",
                    swJ.getTime(TimeUnit.MILLISECONDS),
                    swG.getTime(TimeUnit.MILLISECONDS));

        return Math.random() < 0.5d ?
            gsonResult :
            jsoniterResult; // Avoid the JIT from detecting one of them is not used
    }
}
