package fr.cnes.regards.modules.feature.domain.request;

import java.util.Set;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IAbstractFeatureRequest extends IAbstractRequest {

    Set<String> getErrors();

    String getGroupId();

    Long getId();
}
