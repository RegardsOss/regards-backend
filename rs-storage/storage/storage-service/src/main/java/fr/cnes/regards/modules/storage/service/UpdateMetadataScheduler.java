package fr.cnes.regards.modules.storage.service;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import fr.cnes.regards.modules.storage.dao.IStorageParameterRepository;
import fr.cnes.regards.modules.storage.domain.parameter.StorageParameter;
import fr.cnes.regards.modules.storage.service.parameter.IStorageParameterService;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Configuration
@EnableScheduling
@Order
public class UpdateMetadataScheduler implements SchedulingConfigurer {

    @Autowired
    private IAIPService aipService;

    @Autowired
    private IStorageParameterRepository storageParameterRepo;

    @Bean(destroyMethod = "shutdown")
    public Executor updateMetadataTaskExecutor() {
        return Executors.newScheduledThreadPool(1);
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(updateMetadataTaskExecutor());
        taskRegistrar.addTriggerTask(() -> aipService.updateAlreadyStoredMetadata(), triggerContext -> {
            Calendar nextExecutionTime = new GregorianCalendar();
            Date lastActualExecutionTime = triggerContext.lastActualExecutionTime();
            nextExecutionTime.setTime(lastActualExecutionTime != null ? lastActualExecutionTime : new Date());
            // on first start of the microservice, the database might not have been initialiazed yet,
            // so let set the default value. Otherwise lets get it from the DB
            StorageParameter updateRate;
            if ((updateRate = storageParameterRepo.findOneByName(StorageParameter.UPDATE_RATE)) == null) {
                updateRate = new StorageParameter(StorageParameter.UPDATE_RATE,
                                                  IStorageParameterService.DEFAULT_UPDATE_RATE);
            }
            int updateRateFromDB = Integer.parseInt(updateRate.getValue());
            nextExecutionTime.add(Calendar.MINUTE, updateRateFromDB); //you can get the value from wherever you want
            return nextExecutionTime.getTime();
        });
    }

}
