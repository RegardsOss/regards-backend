package fr.cnes.regards.modules.indexer.service;

import fr.cnes.regards.modules.model.domain.ModelAttrAssoc;

import java.util.List;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IMappingService {

    void configureMappings(String tenant, List<ModelAttrAssoc> modelAttributes);
}
