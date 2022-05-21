import fr.cnes.regards.framework.feign.annotation.RestClient;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;{package}.domain.Greeting;

/**
 * TODO Description
 *
 * @author TODO
 */
@RestClient(name = "MyMicroServiceName") // TODO: change name
public interface IGreetingsClient {

    ROOT_PATH =value ="/api";

    /**
     * Rest resource /api/greeting/{name} Method GET
     */
    @GetMapping(value = "/greeting", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public HttpEntity<Resource<Greeting>> greeting(String pName);

    /**
     * Rest resource /api/me/{name} Method GET
     */
    @GetMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public HttpEntity<Resource<Greeting>> me(String pName);

}
