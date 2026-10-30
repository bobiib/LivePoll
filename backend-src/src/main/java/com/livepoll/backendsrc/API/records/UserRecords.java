package com.livepoll.backendsrc.API.records;

import java.util.List;

public class UserRecords
{
    //Generic Response
    public record ApiResponse<T>(
            boolean successful,
            String message,
            T data
    ){
        //Successful Response
        public static <T> ApiResponse<T> success (String message, T data){
            return new ApiResponse<>(true, message, data);
        }
        public static <T> ApiResponse<T> success (String message){
            return new ApiResponse<>(true, message, null);
        }

        //Unsuccessful Response
        public static <T> ApiResponse<T> error (String message, T data){
            return new ApiResponse<>(false, message, data);
        }
    }

    //Specific Requests
    public record PollRecord(
            String title,
            List<PollQuestionRecord> questions
    ){}
    public record PollQuestionRecord(
            String title,
            String type,
            List<String> options,
            Integer maxAnswers
    ){}

    public record CreatePollResponseRecord(
            String roomCode,
            String pollId
    ){}

    public record SavedPollRecord(
            String id,
            String title,
            String roomCode,
            String status,
            Integer activeQuestionIndex,
            Integer questionCount,
            List<PollQuestionRecord> questions
    ){}

    public record PollMetadataRecord(
            String title,
            String status,
            Integer activeQuestionIndex
    ){}

    public record PollDetailsRecord(
            String title,
            String status,
            Integer activeQuestionIndex,
            List<PollQuestionDetailsRecord> questions
    ){}

    public record PollSessionRecord(
            String id,
            String roomCode,
            String createdAt
    ){}

    public record PollQuestionDetailsRecord(
            String title,
            String type,
            List<String> options,
            Integer maxAnswers,
            List<Integer> votes,
            List<String> answers
    ){}

    public record VoteRequestRecord(
            Integer questionIndex,
            Integer optionIndex,
            List<Integer> optionIndexes,
            String answer,
            String participantId
    ){}

    public record QuestionResultsRecord(
            Integer questionIndex,
            String type,
            List<Integer> votes,
            List<String> answers
    ){}

    public record LiveCommandRecord(
            String command,
            Integer questionIndex
    ){}



    //Specific Responses
    public record SuccesfullAuthResponseRecord(
            String JWTToken,
            String email
    ){}
    public record ErrorResponseRecord(
            String errorCode
    ){}
    public record CredentialsSubmitRequestRecord(
            String email,
            String password
    ){}
}

