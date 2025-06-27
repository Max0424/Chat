package com.max.messenger.handler;

import com.max.messenger.service.SessionService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final SessionService sessionService;
    private final Map<String, CopyOnWriteArrayList<WebSocketSession>> sessionMap = new ConcurrentHashMap<>();


    public ChatWebSocketHandler(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = extractSessionId(session);

        if (!sessionService.roomExists(sessionId)) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Room does not exist"));
            return;
        }

        sessionMap.putIfAbsent(sessionId, new CopyOnWriteArrayList<>());
        List<WebSocketSession> currentSessions = sessionMap.get(sessionId);

        // ✅ Reject third user BEFORE adding to session list
        if (currentSessions.size() >= 2) {
            System.out.println("⚠️ Room has more than 2 users! Sending alerts.");

            for (int i = 0; i < 2; i++) {
                WebSocketSession s = currentSessions.get(i);
                if (s.isOpen()) {
                    try {
                        s.sendMessage(new TextMessage("[THIRD_USER_ALERT]"));
                        System.out.println("⚠️  Sent THIRD_USER_ALERT to: " + s.getId());
                    } catch (IOException e) {
                        System.out.println("⚠️ Could not alert: " + s.getId());
                    }
                }
            }

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        session.close(CloseStatus.POLICY_VIOLATION.withReason("Room is full"));
                    } catch (IOException e) {
                        System.out.println("⚠️ Failed to close 3rd user session: " + session.getId());
                    }
                }
            }, 300);
            return;
        }

        String role = currentSessions.isEmpty() ? "host" : "guest";
        session.getAttributes().put("role", role);

        try {
            session.sendMessage(new TextMessage("[ROLE:" + role + "]"));
            System.out.println("📨 Sent role: " + role + " to session " + session.getId());
        } catch (IOException e) {
            System.out.println("⚠️ Failed to send role to: " + session.getId());
        }

        currentSessions.add(session);
        System.out.println("🟢 Connected to room " + sessionId + ", total: " + currentSessions.size());

        sessionService.incrementClientCount(sessionId);

        if (currentSessions.size() == 2) {
            sessionService.lockRoom(sessionId);
            WebSocketSession host = currentSessions.get(0);
            if (host.isOpen()) {
                host.sendMessage(new TextMessage("[USER_2_JOINED]"));
                host.sendMessage(new TextMessage("[PARTNER_ONLINE]"));
            }
            session.sendMessage(new TextMessage("[PARTNER_ONLINE]"));
        }
    }




    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = extractSessionId(session);
        List<WebSocketSession> participants = sessionMap.getOrDefault(sessionId, new CopyOnWriteArrayList<>());

        boolean wasParticipant = participants.contains(session); // ✅ only notify if it was a real user
        participants.remove(session);

        if (wasParticipant) {
            for (WebSocketSession s : participants) {
                try {
                    if (s.isOpen()) {
                        s.sendMessage(new TextMessage("[PARTNER_OFFLINE]"));
                    }
                } catch (IOException | IllegalStateException e) {
                    System.out.println("⚠️ Tried to send message to closed session: " + s.getId());
                }
            }
        }

        sessionService.decrementClientCount(sessionId);

        if (participants.isEmpty()) {
            sessionMap.remove(sessionId);
            if (sessionService.roomExists(sessionId)) {
                sessionService.removeRoomCompletely(sessionId);
            }
        }
    }



    private String extractSessionId(WebSocketSession session) {
        String path = session.getUri().getPath(); // e.g., /chat/abc123
        return path.substring(path.lastIndexOf("/") + 1);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = extractSessionId(session);
        List<WebSocketSession> participants = sessionMap.getOrDefault(sessionId, new CopyOnWriteArrayList<>());

        for (WebSocketSession s : participants) {
            if (s.isOpen()) {
                s.sendMessage(message);
            }
        }
    }
}
