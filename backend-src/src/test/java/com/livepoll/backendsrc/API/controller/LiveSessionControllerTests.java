package com.livepoll.backendsrc.API.controller;

import com.livepoll.backendsrc.API.records.UserRecords;
import com.livepoll.backendsrc.API.service.PollService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LiveSessionControllerTests {

    @Test
    void broadcastsLiveCommandWhenOwnerCheckPasses() {
        PollService pollService = mock(PollService.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        LiveSessionController controller = new LiveSessionController(pollService, messagingTemplate);
        UserRecords.LiveCommandRecord command = new UserRecords.LiveCommandRecord("START_POLL", 0);
        Principal principal = () -> "host@example.com";

        when(pollService.ApplyLiveCommand("ABC123", command, "host@example.com")).thenReturn(true);

        controller.handleLiveCommand("ABC123", command, principal);

        verify(messagingTemplate).convertAndSend("/topic/poll/ABC123/state", command);
    }

    @Test
    void doesNotBroadcastLiveCommandWhenOwnerCheckFails() {
        PollService pollService = mock(PollService.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        LiveSessionController controller = new LiveSessionController(pollService, messagingTemplate);
        UserRecords.LiveCommandRecord command = new UserRecords.LiveCommandRecord("NEXT_QUESTION", 1);
        Principal principal = () -> "attacker@example.com";

        when(pollService.ApplyLiveCommand("ABC123", command, "attacker@example.com")).thenReturn(false);

        controller.handleLiveCommand("ABC123", command, principal);

        verify(messagingTemplate, never()).convertAndSend("/topic/poll/ABC123/state", command);
    }

    @Test
    void doesNotBroadcastLiveCommandWithoutAuthenticatedPrincipal() {
        PollService pollService = mock(PollService.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        LiveSessionController controller = new LiveSessionController(pollService, messagingTemplate);
        UserRecords.LiveCommandRecord command = new UserRecords.LiveCommandRecord("END_POLL", 2);

        when(pollService.ApplyLiveCommand("ABC123", command, null)).thenReturn(false);

        controller.handleLiveCommand("ABC123", command, null);

        verify(messagingTemplate, never()).convertAndSend("/topic/poll/ABC123/state", command);
    }
}
