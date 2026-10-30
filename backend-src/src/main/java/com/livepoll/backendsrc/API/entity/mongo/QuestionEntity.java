package com.livepoll.backendsrc.API.entity.mongo;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "polls")
public class QuestionEntity {

    public QuestionEntity() {
    }

    public QuestionEntity(String title, String type, List<String> options, Integer maxAnswers) {
        this.title = title;
        this.type = type;
        this.options = options;
        this.maxAnswers = maxAnswers;
        this.votes = new ArrayList<>();
        if (options != null) {
            for (int i = 0; i < options.size(); i++) {
                this.votes.add(0);
            }
        }
        this.answers = new ArrayList<>();
    }

    private String title;
    private String type;

    private String answer;

    private Integer maxAnswers;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> options;

    private List<Integer> votes;

    private List<String> answers;

    private java.util.Set<String> votedParticipants = new java.util.HashSet<>();

    //GETTERS AND SETTERS
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getMaxAnswers() { return maxAnswers; }
    public void setMaxAnswers(Integer maxAnswers) { this.maxAnswers = maxAnswers; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    public List<Integer> getVotes() { return votes; }
    public void setVotes(List<Integer> votes) { this.votes = votes; }

    public List<String> getAnswers() { return answers; }
    public void setAnswers(List<String> answers) { this.answers = answers; }

    public java.util.Set<String> getVotedParticipants() {
        if (votedParticipants == null) {
            votedParticipants = new java.util.HashSet<>();
        }
        return votedParticipants;
    }
    public void setVotedParticipants(java.util.Set<String> votedParticipants) { this.votedParticipants = votedParticipants; }
}

