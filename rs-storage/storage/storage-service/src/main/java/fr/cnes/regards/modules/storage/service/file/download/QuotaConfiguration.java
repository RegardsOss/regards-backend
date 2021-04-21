package fr.cnes.regards.modules.storage.service.file.download;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class QuotaConfiguration {

    @Configuration
    public static class QuotaManagerConfiguration {

        public static final String RATE_EXPIRATION_TICKING_SCHEDULER = "rateExpirationTickingScheduler";
        public static final String SYNC_TICKING_SCHEDULER = "syncTickingScheduler";

        @Qualifier(RATE_EXPIRATION_TICKING_SCHEDULER)
        @Bean
        public ThreadPoolTaskScheduler rateExpirationTickingScheduler() {
            return new ThreadPoolTaskScheduler();
        }

        @Qualifier(SYNC_TICKING_SCHEDULER)
        @Bean
        public ThreadPoolTaskScheduler syncTickingScheduler() {
            return new ThreadPoolTaskScheduler();
        }

    }

    @Configuration
    public static class QuotaExceededReporterConfiguration {

        public static final String REPORT_TICKING_SCHEDULER = "reportTickingScheduler";

        @Qualifier(REPORT_TICKING_SCHEDULER)
        @Bean
        public ThreadPoolTaskScheduler reportTickingScheduler() {
            return new ThreadPoolTaskScheduler();
        }

    }
}
