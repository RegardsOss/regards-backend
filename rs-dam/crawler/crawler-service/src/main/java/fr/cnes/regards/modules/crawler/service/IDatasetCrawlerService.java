package fr.cnes.regards.modules.crawler.service;

import fr.cnes.regards.modules.model.gson.ModelJsonReadyEvent;
import fr.cnes.regards.modules.model.service.event.ComputedAttributeModelEvent;

/**
 * @author oroussel
 */
public interface IDatasetCrawlerService extends ICrawlerService {

    void onApplicationReadyEvent(ModelJsonReadyEvent event);

    void onComputedAttributeModelEvent(ComputedAttributeModelEvent event);
}
