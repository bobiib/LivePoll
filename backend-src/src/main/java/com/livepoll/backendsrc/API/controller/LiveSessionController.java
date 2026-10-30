package com.livepoll.backendsrc.API.controller;

import com.livepoll.backendsrc.API.records.UserRecords;
import com.livepoll.backendsrc.API.service.PollService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class LiveSessionController {

    private final PollService pollService;
    private final SimpMessagingTemplate messagingTemplate;

    public LiveSessionController(PollService pollService, SimpMessagingTemplate messagingTemplate) {
        this.pollService = pollService;
        this.messagingTemplate = messagingTemplate;
    }

    // The Host sends commands to /app/poll/{roomCode}/command
    // This broadcasts the command to all participants subscribed to /topic/poll/{roomCode}/state
    @MessageMapping("/poll/{roomCode}/command")
    public void handleLiveCommand(
            @DestinationVariable String roomCode,
            UserRecords.LiveCommandRecord command,
            Principal principal
    ) {
        String hostEmail = principal == null ? null : principal.getName();
        boolean applied = pollService.ApplyLiveCommand(roomCode, command, hostEmail);
        if (applied) {
            messagingTemplate.convertAndSend("/topic/poll/" + roomCode + "/state", command);
        }
    }
}
