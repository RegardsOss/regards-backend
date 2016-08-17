package fr.cnes.regards.microservices.backend.controllers.sandbox;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

public class TimeController extends Controller {

	private static SendTime timeThread_ = null;

	/**
	 * Method to start the web socket server timer. The timer send current date
	 * every 2 seconds to the web sockets servers
	 *
	 * @return
	 */
	@RequestMapping(value = "time/start", method = RequestMethod.GET)
	public @ResponseBody String startTime() {
		if (timeThread_ == null) {
			System.out.println("starting time thread !!!!");
			timeThread_ = new SendTime(this);
			timeThread_.run();
		}
		return "OK";
	}

}
