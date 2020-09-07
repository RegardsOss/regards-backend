package fr.cnes.regards.modules.processing.config;

import com.google.gson.Gson;
import fr.cnes.regards.modules.processing.utils.gson.GsonInefficientHttpMessageCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class ProcessingServerJsonConfiguration implements WebFluxConfigurer {

    @Autowired
    private Gson gson;

    @Override public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.customCodecs().register(new GsonInefficientHttpMessageCodec.Co(gson));
        configurer.customCodecs().register(new GsonInefficientHttpMessageCodec.Dec(gson));
    }
}
