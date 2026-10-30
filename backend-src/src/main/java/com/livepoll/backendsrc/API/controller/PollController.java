package com.livepoll.backendsrc.API.controller;

import com.livepoll.backendsrc.API.records.UserRecords;
import com.livepoll.backendsrc.API.service.PollService;
import com.livepoll.backendsrc.API.service.security.RoomCodeAccessGuard;
import com.livepoll.backendsrc.API.service.tokens.TokenService;
import com.livepoll.backendsrc.Util.RetrievalUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/polls")
public class PollController {

    private final PollService pollService;
    private final TokenService tokenService;
    private final RetrievalUtil retrievalUtil;
    private final RoomCodeAccessGuard roomCodeAccessGuard;


    public PollController(PollService pollService, TokenService tokenService, RetrievalUtil retrievalUtil, RoomCodeAccessGuard roomCodeAccessGuard) {
        this.pollService = pollService;
        this.tokenService = tokenService;
        this.retrievalUtil = retrievalUtil;
        this.roomCodeAccessGuard = roomCodeAccessGuard;
    }

    @PostMapping
    public ResponseEntity<UserRecords.ApiResponse<UserRecords.CreatePollResponseRecord>> CreatePoll(@RequestBody UserRecords.PollRecord pollRequest, @RequestHeader("Authorization") String authHeader){
        return pollService.CreatePoll(pollRequest, retrievalUtil.getUserEntityFromToken(tokenService.parseTokenFromHeader(authHeader)));
    }

    @GetMapping("get-polls")
    public ResponseEntity<UserRecords.ApiResponse<List<UserRecords.SavedPollRecord>>> GetPolls(@RequestHeader("Authorization") String authHeader){
        return pollService.GetPolls(retrievalUtil.getUserEntityFromToken(tokenService.parseTokenFromHeader(authHeader)));
    }

    @GetMapping("/saved/{pollId}")
    public ResponseEntity<UserRecords.ApiResponse<UserRecords.SavedPollRecord>> GetSavedPoll(
            @PathVariable String pollId,
            @RequestHeader("Authorization") String authHeader
    ){
        return pollService.GetSavedPoll(pollId, retrievalUtil.getUserEntityFromToken(tokenService.parseTokenFromHeader(authHeader)));
    }

    @PostMapping("/{pollId}/start")
    public ResponseEntity<UserRecords.ApiResponse<UserRecords.CreatePollResponseRecord>> StartSavedPoll(
            @PathVariable String pollId,
            @RequestHeader("Authorization") String authHeader
    ){
        return pollService.StartSavedPoll(pollId, retrievalUtil.getUserEntityFromToken(tokenService.parseTokenFromHeader(authHeader)));
    }

    @GetMapping("/{pollId}/sessions")
    public ResponseEntity<UserRecords.ApiResponse<List<UserRecords.PollSessionRecord>>> GetPollHistory(
            @PathVariable String pollId,
            @RequestHeader("Authorization") String authHeader
    ){
        return pollService.GetPollHistory(pollId, retrievalUtil.getUserEntityFromToken(tokenService.parseTokenFromHeader(authHeader)));
    }

    @PutMapping("/{pollId}")
    public ResponseEntity<UserRecords.ApiResponse<UserRecords.SavedPollRecord>> UpdatePoll(
            @PathVariable String pollId,
            @RequestBody UserRecords.PollRecord pollRequest,
            @RequestHeader("Authorization") String authHeader
    ){
        return pollService.UpdatePoll(pollId, pollRequest, retrievalUtil.getUserEntityFromToken(tokenService.parseTokenFromHeader(authHeader)));
    }

    @DeleteMapping("/{pollId}")
    public ResponseEntity<UserRecords.ApiResponse<Void>> DeletePoll(
            @PathVariable String pollId,
            @RequestHeader("Authorization") String authHeader
    ){
        return pollService.DeletePoll(pollId, retrievalUtil.getUserEntityFromToken(tokenService.parseTokenFromHeader(authHeader)));
    }

    @GetMapping("/{roomCode}")
    public ResponseEntity<UserRecords.ApiResponse<UserRecords.PollMetadataRecord>> GetPollByRoomCode(@PathVariable String roomCode, HttpServletRequest request){
        ResponseEntity<UserRecords.ApiResponse<UserRecords.PollMetadataRecord>> guardResponse = guardPublicRoomLookup(roomCode, request);
        if (guardResponse != null) {
            return guardResponse;
        }

        return pollService.GetPollMetadataByRoomCode(roomCode);
    }

    @GetMapping("/{roomCode}/details")
    public ResponseEntity<UserRecords.ApiResponse<UserRecords.PollDetailsRecord>> GetPollDetailsByRoomCode(@PathVariable String roomCode, HttpServletRequest request){
        ResponseEntity<UserRecords.ApiResponse<UserRecords.PollDetailsRecord>> guardResponse = guardPublicRoomLookup(roomCode, request);
        if (guardResponse != null) {
            return guardResponse;
        }

        return pollService.GetPollDetailsByRoomCode(roomCode);
    }

    @PostMapping("/{roomCode}/vote")
    public ResponseEntity<UserRecords.ApiResponse<UserRecords.QuestionResultsRecord>> SubmitVote(
            @PathVariable String roomCode,
            @RequestBody UserRecords.VoteRequestRecord voteRequest
    ){
        return pollService.SubmitVote(roomCode, voteRequest);
    }

    private <T> ResponseEntity<UserRecords.ApiResponse<T>> guardPublicRoomLookup(String roomCode, HttpServletRequest request) {
        if (!roomCodeAccessGuard.isValidRoomCode(roomCode)) {
            return ResponseEntity.badRequest().body(UserRecords.ApiResponse.error("Invalid room code.", null));
        }

        if (!roomCodeAccessGuard.allowLookup(request)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(UserRecords.ApiResponse.error("Too many room code requests. Try again later.", null));
        }

        return null;
    }
}
