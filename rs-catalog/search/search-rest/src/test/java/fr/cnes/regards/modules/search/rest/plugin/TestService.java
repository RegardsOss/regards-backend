/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest.plugin;

import java.util.Set;

import org.assertj.core.util.Sets;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.models.domain.EntityType;
import fr.cnes.regards.modules.models.domain.Model;
import fr.cnes.regards.modules.search.domain.IService;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Plugin(author = "Sylvain Vissiere-Guerinet")
public class TestService implements IService {

    public static final String EXPECTED_VALUE = "skydiving";

    @PluginParameter(name = "para")
    private String para;

    @Override
    public Set<DataObject> apply(String pQuery) {
        if (!para.equals(EXPECTED_VALUE)) {
            return Sets.newHashSet();
        }
        Model model = Model.build("pName", "pDescription", EntityType.DATA);
        DataObject do1 = new DataObject(model, "pTenant", "pLabel1");
        DataObject do2 = new DataObject(model, "pTenant", "pLabel2");
        return Sets.newLinkedHashSet(do1, do2);
    }

}
