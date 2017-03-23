/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest.plugin;

import java.util.Set;

import org.assertj.core.util.Sets;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
@Plugin(author = "Sylvain Vissiere-Guerinet", id = "tata")
public class TestService implements IService {

    public static final String EXPECTED_VALUE = "skydiving";

    @PluginParameter(name = "para")
    private String para;

    @Override
    public ResponseEntity<?> apply() {
        if (!para.equals(EXPECTED_VALUE)) {
            return new ResponseEntity<Set<DataObject>>(Sets.newHashSet(), HttpStatus.OK);
        }
        Model model = Model.build("pName", "pDescription", EntityType.DATA);
        DataObject do1 = new DataObject(model, "pTenant", "pLabel1");
        DataObject do2 = new DataObject(model, "pTenant", "pLabel2");
        return new ResponseEntity<Set<DataObject>>(Sets.newLinkedHashSet(do1, do2), HttpStatus.OK);
    }

    @Override
    public boolean isApplyableOnOneData() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isApplyableOnManyData() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isApplyableOnQuery() {
        // TODO Auto-generated method stub
        return false;
    }

}
