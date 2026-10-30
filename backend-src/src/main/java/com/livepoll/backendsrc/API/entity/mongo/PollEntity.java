package com.livepoll.backendsrc.API.entity.mongo;

import jakarta.persistence.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.UUID;

@Document("polls")
public class PollEntity {

    public PollEntity(UUID user_id, String title, List<QuestionEntity> questions, String roomCode) {
        this.userID = user_id;
        this.title = title;
        this.questions = questions;
        this.roomCode = roomCode;
        this.status = "LOBBY";
        this.activeQuestionIndex = 0;
    }

    public PollEntity() {}


    @Id
    private String id;

    private UUID userID;

    private String title;

    private String roomCode;

    private String status;

    private Integer activeQuestionIndex;

    private List<QuestionEntity> questions;

    // GETTERS AND SETTERS
    public String getId() { return id; }

    public UUID getUserID() { return userID; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getActiveQuestionIndex() { return activeQuestionIndex; }
    public void setActiveQuestionIndex(Integer activeQuestionIndex) { this.activeQuestionIndex = activeQuestionIndex; }

    public List<QuestionEntity> getQuestions() { return questions; }
    public void setQuestions(List<QuestionEntity> questions) { this.questions = questions; }

}
