/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.access.signature;

import java.util.List;

import feign.Param;
import feign.RequestLine;
import fr.cnes.regards.modules.access.domain.NavigationContext;


/**
 * 
 * @author cmertz
 *
 */
public interface INavigationContextSignature {

    @RequestLine("GET /tiny/url/{pTinyUrl}")
    NavigationContext load(@Param("pTinyUrl") String pTinyUrl);

    @RequestLine("PUT /tiny/url/{pTinyUrl}")
    void update(@Param("pTinyUrl") String pTinyUrl);

    @RequestLine("DELETE /tiny/url/{pTinyUrl}")
    void delete(@Param("pTinyUrl") String pTinyUrl);

    @RequestLine("GET /tiny/urls")
    List<NavigationContext> list();

    @RequestLine("POST /tiny/urls")
    NavigationContext create(NavigationContext pNavigationContext);

}
