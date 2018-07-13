package fr.cnes.regards.modules.search.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineConfiguration;
import fr.cnes.regards.modules.search.service.ISearchEngineConfigurationService;

@RestController
@RequestMapping(path = SearchEngineConfigurationController.TYPE_MAPPING)
public class SearchEngineConfigurationController implements IResourceController<SearchEngineConfiguration> {

    public static final String TYPE_MAPPING = "enginesconfig";

    @Autowired
    private ISearchEngineConfigurationService service;

    @Override
    public Resource<SearchEngineConfiguration> toResource(SearchEngineConfiguration element, Object... extras) {
        // TODO Auto-generated method stub
        return null;
    }

}
