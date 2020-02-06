package tech.miniswp.rs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;

@Component
public class OutputWSRulesHandler extends TextWebSocketHandler {

    private final Logger log = LoggerFactory.getLogger(OutputWSRulesHandler.class);
    private HashMap<String, WebSocketSession> sesMap = new HashMap<String, WebSocketSession>();

    @Autowired
    RulesServiceEng re;


    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {

        log.info(message.getPayload());
        String res = re.execRules("", message.getPayload());
        sendToAll(res);

    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable throwable) throws Exception {
        log.error("error occured at sender " + session, throwable);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {

        log.info(String.format("Session %s closed because of %s", session.getId(), status.getReason()));

    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sesMap.put(session.getId(), session);
        //mon.updateWebpage(session);

        log.info("Connected ... " + session.getId());
    }

    public void sendToAll(String msg){
        for (WebSocketSession ses : sesMap.values()){
            try {
                if(ses.isOpen()) {
                    ses.sendMessage(new TextMessage(msg));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void updateWebPage(WebSocketSession session, String text ) {
        try {
            session.sendMessage( new TextMessage(text));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

