package fr.cnes.regards.modules.ltamanager.dto.submission;

import fr.cnes.regards.framework.urn.DataType;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author Sébastien Binda
 **/
public class LtaDataTypeTest {

    @Test
    public void test_data_types_coherence() {
        Arrays.stream(DataType.values()).forEach(dt -> {
            Assert.assertNotNull(String.format("Missing datatype %s", dt), LtaDataType.parse(dt.toString(), null));
        });
    }

}
