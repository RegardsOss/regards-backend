package fr.cnes.regards.modules.model.domain.attributes;

import com.google.gson.Gson;
import fr.cnes.regards.modules.model.domain.attributes.restriction.EnumerationRestriction;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class AttributeModelTest {

    private Gson gson = new Gson();

    @Test
    public void testJson() {
        AttributeModel attr = new AttributeModel();
        attr.setId(123456789L);
        attr.setAlterable(false);
        attr.setArraysize(0);
        attr.setDescription("Attribute description.");
        attr.setDynamic(false);
        attr.setGroup("attr-group");
        attr.setInternal(false);
        attr.setJsonPath("context.name");
        attr.setLabel("ATTR LABEL");
        attr.setName("The attribute name");
        attr.setOptional(false);
        attr.setType(PropertyType.DATE_INTERVAL);
        attr.setRestriction(new EnumerationRestriction());
        attr.setAlterable(false);
        attr.setUnit("instant");
        System.out.println(gson.toJson(attr.toXml()));
    }

}