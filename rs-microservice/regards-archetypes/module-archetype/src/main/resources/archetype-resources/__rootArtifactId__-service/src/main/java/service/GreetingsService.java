import org.springframework.stereotype.Service;{package}.domain.Greeting;

/**
 * TODO Description
 *
 * @author TODO
 */
@Service
public class GreetingsService implements IGreetingsService {

    @Override
    public Greeting getGreeting(String pName) {
        return new Greeting(pName);
    }

}
