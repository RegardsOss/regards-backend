/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.plugins.utils.bean;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 *
 * The implementation of {@link ISampleBeanService}.
 *
 * @author Christophe Mertz
 */
@Service
@Primary
public class SampleBeanService implements ISampleBeanService {

    /**
     * A string attribute
     */
    private String str;

    public String getId() {
        return str;
    }

    @Override
    public void setId(String pId) {
        str = pId;
    }

}
