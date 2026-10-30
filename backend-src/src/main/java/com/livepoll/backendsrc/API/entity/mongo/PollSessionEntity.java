package com.livepoll.backendsrc.API.entity.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document("poll_sessions")
public class PollSessionEntity {

    @Id
    private String id;

    private String pollId;
    private String roomCode;
    private Instant createdAt;
    private List<QuestionEntity> questions;

    public PollSessionEntity() {
    }

    public PollSessionEntity(String pollId, String roomCode, Instant createdAt, List<QuestionEntity> questions) {
        this.pollId = pollId;
        this.roomCode = roomCode;
        this.createdAt = createdAt;
        this.questions = questions;
    }

    public String getId() { return id; }
    public String getPollId() { return pollId; }
    public String getRoomCode() { return roomCode; }
    public Instant getCreatedAt() { return createdAt; }
    public List<QuestionEntity> getQuestions() { return questions; }

    public void setPollId(String pollId) { this.pollId = pollId; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setQuestions(List<QuestionEntity> questions) { this.questions = questions; }
}
