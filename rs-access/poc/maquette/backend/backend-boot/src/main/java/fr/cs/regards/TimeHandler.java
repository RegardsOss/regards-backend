package fr.cs.regards;

import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class TimeHandler extends TextWebSocketHandler {
	@Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
		System.out.println("handleMessage : " + message.toString());
    }

}
