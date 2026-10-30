package com.livepoll.backendsrc.API.service;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class LiveSessionEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    // Maps sessionId -> roomCode
    private final ConcurrentHashMap<String, String> sessionRoomMap = new ConcurrentHashMap<>();
    
    // Maps sessionId -> participantId
    private final ConcurrentHashMap<String, String> sessionParticipantMap = new ConcurrentHashMap<>();
    
    // Maps roomCode -> participantId -> Set of sessionIds
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, java.util.Set<String>>> roomParticipantSessions = new ConcurrentHashMap<>();

    public LiveSessionEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        String participantId = headerAccessor.getFirstNativeHeader("participantId");

        if (destination != null && destination.startsWith("/topic/poll/") && destination.endsWith("/state")) {
            String[] parts = destination.split("/");
            if (parts.length >= 5) {
                String roomCode = parts[3];
                String existingRoomCode = sessionRoomMap.putIfAbsent(sessionId, roomCode);
                if (existingRoomCode != null) {
                    sendParticipantCount(existingRoomCode);
                    return;
                }

                if (participantId != null) {
                    sessionParticipantMap.put(sessionId, participantId);
                    roomParticipantSessions.computeIfAbsent(roomCode, k -> new ConcurrentHashMap<>())
                            .computeIfAbsent(participantId, k -> ConcurrentHashMap.newKeySet())
                            .add(sessionId);
                }

                sendParticipantCount(roomCode);
            }
        } else if (destination != null && destination.startsWith("/topic/poll/") && destination.endsWith("/host")) {
            String[] parts = destination.split("/");
            if (parts.length >= 5) {
                sendParticipantCount(parts[3]);
            }
        }
    }

    @EventListener
    public void handleSessionDisconnectEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        String roomCode = sessionRoomMap.remove(sessionId);
        String participantId = sessionParticipantMap.remove(sessionId);

        if (roomCode != null) {
            if (participantId != null) {
                ConcurrentHashMap<String, java.util.Set<String>> participants = roomParticipantSessions.get(roomCode);
                if (participants != null) {
                    java.util.Set<String> sessions = participants.get(participantId);
                    if (sessions != null) {
                        sessions.remove(sessionId);
                        if (sessions.isEmpty()) {
                            participants.remove(participantId);
                        }
                    }
                }
            }
            sendParticipantCount(roomCode);
        }
    }

    private void sendParticipantCount(String roomCode) {
        ConcurrentHashMap<String, java.util.Set<String>> participants = roomParticipantSessions.get(roomCode);
        int count = (participants != null) ? participants.size() : 0;
        messagingTemplate.convertAndSend("/topic/poll/" + roomCode + "/host", count);
    }
}
