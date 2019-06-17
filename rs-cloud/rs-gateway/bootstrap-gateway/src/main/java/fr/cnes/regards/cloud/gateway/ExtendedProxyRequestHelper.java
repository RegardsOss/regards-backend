package fr.cnes.regards.cloud.gateway;

import com.netflix.zuul.context.RequestContext;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import javax.servlet.http.HttpServletRequest;

@Service
public class ExtendedProxyRequestHelper extends ProxyRequestHelper
{
    public ExtendedProxyRequestHelper(ZuulProperties zuulProperties) {
        super(zuulProperties);
    }

    @Override
    public String getQueryString(MultiValueMap<String, String> params)
    {
        HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
        String query = request.getQueryString();
        return (query != null) ? "?" + query : "";
    }
}
