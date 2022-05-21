package fr.cnes.regards.modules.indexer.dao.deser;

import com.google.gson.Gson;
import fr.cnes.regards.framework.jsoniter.IIndexableJsoniterConfig;
import fr.cnes.regards.modules.indexer.domain.IIndexable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonDeserializeStrategyConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonDeserializeStrategyConfig.class);

    public static final String JSON_DESERIALIZE_STRATEGY_INSTANCE_IS = "JsonDeserializeStrategy instance is {}";

    @Bean
    @ConditionalOnExpression("'${regards.elasticsearch.deserialize.hits.strategy}'.equals('GSON')")
    public JsonDeserializeStrategy<IIndexable> gsonDeserializeHitsStrategyIIndexable(Gson gson) {
        GsonDeserializeIIndexableStrategy result = new GsonDeserializeIIndexableStrategy(gson);
        LOGGER.info(JSON_DESERIALIZE_STRATEGY_INSTANCE_IS, result);
        return result;
    }

    @Bean
    @ConditionalOnExpression("'${regards.elasticsearch.deserialize.hits.strategy}'.equals('BOTH')")
    public JsonDeserializeStrategy<IIndexable> bothDeserializeHitsStrategyIIndexable(IIndexableJsoniterConfig config,
                                                                                     Gson gson) {
        BothForProfilingJsonDeserializeStrategyConfig result = new BothForProfilingJsonDeserializeStrategyConfig(new GsonDeserializeIIndexableStrategy(
            gson), new JsoniterDeserializeIIndexableStrategy(config));
        LOGGER.info(JSON_DESERIALIZE_STRATEGY_INSTANCE_IS, result);
        return result;
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonDeserializeStrategy<IIndexable> jsoniterDeserializeHitsStrategyIIndexable(IIndexableJsoniterConfig config) {
        JsoniterDeserializeIIndexableStrategy result = new JsoniterDeserializeIIndexableStrategy(config);
        LOGGER.info(JSON_DESERIALIZE_STRATEGY_INSTANCE_IS, result);
        return result;
    }

}
