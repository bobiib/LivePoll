package com.livepoll.backendsrc.API.service;

import com.livepoll.backendsrc.API.entity.jpa.UserEntity;
import com.livepoll.backendsrc.API.entity.mongo.PollEntity;
import com.livepoll.backendsrc.API.entity.mongo.QuestionEntity;
import com.livepoll.backendsrc.API.records.UserRecords;
import com.livepoll.backendsrc.API.repository.jpa.UserRepository;
import com.livepoll.backendsrc.API.entity.mongo.PollSessionEntity;
import com.livepoll.backendsrc.API.repository.mongo.PollRepository;
import com.livepoll.backendsrc.API.repository.mongo.PollSessionRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Service
public class PollService {

    private static final String ROOM_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final PollRepository pollRepository;
    private final PollSessionRepository pollSessionRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public PollService(PollRepository pollRepository, PollSessionRepository pollSessionRepository, UserRepository userRepository, SimpMessagingTemplate messagingTemplate) {
        this.pollRepository = pollRepository;
        this.pollSessionRepository = pollSessionRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }


    private String generateRoomCode() {
        StringBuilder code = new StringBuilder();
        while (code.length() < 6) {
            int index = secureRandom.nextInt(ROOM_CODE_CHARS.length());
            code.append(ROOM_CODE_CHARS.charAt(index));
        }
        return code.toString();
    }

    @Transactional
    public ResponseEntity<UserRecords.ApiResponse<UserRecords.CreatePollResponseRecord>> CreatePoll(UserRecords.PollRecord pollRequest, UserEntity user){
        if (pollRequest == null) {
            return ResponseEntity.badRequest().body(UserRecords.ApiResponse.error("Poll request is required.", null));
        }

        if (pollRequest.title() == null || pollRequest.title().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(UserRecords.ApiResponse.error("Poll title is required.", null));
        }

        if (pollRequest.questions() == null || pollRequest.questions().isEmpty()) {
            return ResponseEntity.badRequest().body(UserRecords.ApiResponse.error("At least one question is required.", null));
        }

        for (UserRecords.PollQuestionRecord q : pollRequest.questions()) {
            if (q.title() == null || q.title().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(UserRecords.ApiResponse.error("All questions must have a title.", null));
            }
            if (!"text".equals(q.type()) && !"single".equals(q.type()) && !"multi".equals(q.type())) {
                return ResponseEntity.badRequest().body(UserRecords.ApiResponse.error("Invalid question type.", null));
            }
            if (!"text".equals(q.type())) {
                long validOptions = q.options() == null ? 0 : q.options().stream().filter(opt -> opt != null && !opt.trim().isEmpty()).count();
                if (validOptions < 2) {
                    return ResponseEntity.badRequest().body(UserRecords.ApiResponse.error("Choice questions must have at least two options.", null));
                }
                if ("multi".equals(q.type())) {
                    if (q.maxAnswers() == null || q.maxAnswers() < 1 || q.maxAnswers() > validOptions) {
                        return ResponseEntity.badRequest().body(UserRecords.ApiResponse.error("Invalid max answers for multiple choice question.", null));
                    }
                }
            }
        }

        UUID user_id = user.getId();

        List<QuestionEntity> questionList = mapPollRequestQuestions(pollRequest);

        String roomCode = generateRoomCode();
        while (pollRepository.findByRoomCode(roomCode).isPresent()) {
            roomCode = generateRoomCode();
        }

        PollEntity poll = new PollEntity(
                user_id,
                pollRequest.title(),
                questionList,
                roomCode
        );

        pollRepository.save(poll);

        return ResponseEntity.ok(UserRecords.ApiResponse.success("Successfully created Poll.", new UserRecords.CreatePollResponseRecord(roomCode, poll.getId())));
    }

    @Transactional
    public ResponseEntity<UserRecords.ApiResponse<List<UserRecords.SavedPollRecord>>> GetPolls(UserEntity user){
        List<PollEntity> pollEntityList = pollRepository.findPollEntitiesByUserID(user.getId());

        List<UserRecords.SavedPollRecord> PollList = new ArrayList<>();

        for (PollEntity pollEntity : pollEntityList){
            PollList.add(mapSavedPoll(pollEntity));
        }
        return ResponseEntity.ok(UserRecords.ApiResponse.success("Successfully retrieved Users Polls", PollList));
    }

    @Transactional
    public ResponseEntity<UserRecords.ApiResponse<UserRecords.SavedPollRecord>> GetSavedPoll(String pollId, UserEntity user){
        Optional<PollEntity> optionalPoll = pollRepository.findById(pollId);
        if (optionalPoll.isEmpty()) {
            return ResponseEntity.status(404).body(UserRecords.ApiResponse.error("Poll not found.", null));
        }

        PollEntity poll = optionalPoll.get();
        if (!poll.getUserID().equals(user.getId())) {
            return ResponseEntity.status(403).body(UserRecords.ApiResponse.error("You cannot edit this poll.", null));
        }

        return ResponseEntity.ok(UserRecords.ApiResponse.success("Successfully retrieved Poll.", mapSavedPoll(poll)));
    }

    @Transactional
    public ResponseEntity<UserRecords.ApiResponse<UserRecords.CreatePollResponseRecord>> StartSavedPoll(String pollId, UserEntity user){
        Optional<PollEntity> optionalPoll = pollRepository.findById(pollId);
        if (optionalPoll.isEmpty()) {
            return ResponseEntity.status(404).body(UserRecords.ApiResponse.error("Poll not found.", null));
        }

        PollEntity poll = optionalPoll.get();
        if (!poll.getUserID().equals(user.getId())) {
            return ResponseEntity.status(403).body(UserRecords.ApiResponse.error("You cannot start this poll.", null));
        }

        String roomCode = generateRoomCode();
        while (pollRepository.findByRoomCode(roomCode).isPresent()) {
            roomCode = generateRoomCode();
        }

        boolean hasVotes = poll.getQuestions().stream()
                .anyMatch(q -> q.getVotedParticipants() != null && !q.getVotedParticipants().isEmpty());
        if (hasVotes || "FINISHED".equals(poll.getStatus()) || "LIVE".equals(poll.getStatus())) {
            PollSessionEntity session = new PollSessionEntity(
                    poll.getId(),
                    poll.getRoomCode(),
                    java.time.Instant.now(),
                    cloneQuestionsWithVotes(poll.getQuestions())
            );
            pollSessionRepository.save(session);
        }

        poll.setRoomCode(roomCode);
        poll.setStatus("LOBBY");
        poll.setActiveQuestionIndex(0);
        poll.setQuestions(resetQuestions(poll.getQuestions()));
        pollRepository.save(poll);

        return ResponseEntity.ok(UserRecords.ApiResponse.success("Successfully started saved Poll.", new UserRecords.CreatePollResponseRecord(roomCode, poll.getId())));
    }

    @Transactional
    public ResponseEntity<UserRecords.ApiResponse<UserRecords.SavedPollRecord>> UpdatePoll(String pollId, UserRecords.PollRecord pollRequest, UserEntity user){
        if (pollRequest == null) {
            return ResponseEntity.badRequest().body(UserRecords.ApiResponse.error("Poll request is required.", null));
        }

        if (pollRequest.title() == null || pollRequest.title().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(UserRecords.ApiResponse.error("Poll title is required.", null));
        }

        if (pollRequest.questions() == null || pollRequest.questions().isEmpty()) {
            return ResponseEntity.badRequest().body(UserRecords.ApiResponse.error("At least one question is required.", null));
        }

        for (UserRecords.PollQuestionRecord q : pollRequest.questions()) {
            if (q.title() == null || q.title().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(UserRecords.ApiResponse.error("All questions must have a title.", null));
            }
            if (!"text".equals(q.type()) && !"single".equals(q.type()) && !"multi".equals(q.type())) {
                return ResponseEntity.badRequest().body(UserRecords.ApiResponse.error("Invalid question type.", null));
            }
            if (!"text".equals(q.type())) {
                long validOptions = q.options() == null ? 0 : q.options().stream().filter(opt -> opt != null && !opt.trim().isEmpty()).count();
                if (validOptions < 2) {
                    return ResponseEntity.badRequest().body(UserRecords.ApiResponse.error("Choice questions must have at least two options.", null));
                }
                if ("multi".equals(q.type())) {
                    if (q.maxAnswers() == null || q.maxAnswers() < 1 || q.maxAnswers() > validOptions) {
                        return ResponseEntity.badRequest().body(UserRecords.ApiResponse.error("Invalid max answers for multiple choice question.", null));
                    }
                }
            }
        }

        Optional<PollEntity> optionalPoll = pollRepository.findById(pollId);
        if (optionalPoll.isEmpty()) {
            return ResponseEntity.status(404).body(UserRecords.ApiResponse.error("Poll not found.", null));
        }

        PollEntity poll = optionalPoll.get();
        if (!poll.getUserID().equals(user.getId())) {
            return ResponseEntity.status(403).body(UserRecords.ApiResponse.error("You cannot edit this poll.", null));
        }

        poll.setTitle(pollRequest.title());
        poll.setQuestions(mapPollRequestQuestions(pollRequest));
        poll.setStatus("LOBBY");
        poll.setActiveQuestionIndex(0);
        pollRepository.save(poll);

        return ResponseEntity.ok(UserRecords.ApiResponse.success("Successfully updated Poll.", mapSavedPoll(poll)));
    }

    @Transactional
    public ResponseEntity<UserRecords.ApiResponse<Void>> DeletePoll(String pollId, UserEntity user){
        Optional<PollEntity> optionalPoll = pollRepository.findById(pollId);
        if (optionalPoll.isEmpty()) {
            return ResponseEntity.status(404).body(UserRecords.ApiResponse.error("Poll not found.", null));
        }

        PollEntity poll = optionalPoll.get();
        if (!poll.getUserID().equals(user.getId())) {
            return ResponseEntity.status(403).body(UserRecords.ApiResponse.error("You cannot delete this poll.", null));
        }

        pollRepository.delete(poll);

        return ResponseEntity.ok(UserRecords.ApiResponse.<Void>success("Successfully deleted Poll.", null));
    }

    public ResponseEntity<UserRecords.ApiResponse<UserRecords.PollMetadataRecord>> GetPollMetadataByRoomCode(String roomCode) {
        Optional<PollEntity> optionalPoll = findByRoomCode(roomCode);
        if (optionalPoll.isEmpty()) {
            return ResponseEntity.status(404).body(UserRecords.ApiResponse.error("Room not found.", null));
        }
        PollEntity poll = optionalPoll.get();
        return ResponseEntity.ok(UserRecords.ApiResponse.success("Room found.", new UserRecords.PollMetadataRecord(
                poll.getTitle(),
                poll.getStatus(),
                currentQuestionIndex(poll)
        )));
    }

    @Transactional
    public ResponseEntity<UserRecords.ApiResponse<List<UserRecords.PollSessionRecord>>> GetPollHistory(String pollId, UserEntity user) {
        Optional<PollEntity> optionalPoll = pollRepository.findById(pollId);
        if (optionalPoll.isEmpty()) {
            return ResponseEntity.status(404).body(UserRecords.ApiResponse.error("Poll not found.", null));
        }

        PollEntity poll = optionalPoll.get();
        if (!poll.getUserID().equals(user.getId())) {
            return ResponseEntity.status(403).body(UserRecords.ApiResponse.error("You cannot view this poll's history.", null));
        }

        List<PollSessionEntity> sessions = pollSessionRepository.findByPollIdOrderByCreatedAtDesc(pollId);
        List<UserRecords.PollSessionRecord> records = new ArrayList<>();

        boolean hasVotes = poll.getQuestions().stream()
                .anyMatch(q -> q.getVotedParticipants() != null && !q.getVotedParticipants().isEmpty());

        if ("FINISHED".equals(poll.getStatus()) || hasVotes) {
            records.add(new UserRecords.PollSessionRecord(
                    poll.getId(),
                    poll.getRoomCode(),
                    java.time.Instant.now().toString()
            ));
        }

        sessions.forEach(s -> records.add(new UserRecords.PollSessionRecord(s.getId(), s.getRoomCode(), s.getCreatedAt().toString())));

        return ResponseEntity.ok(UserRecords.ApiResponse.success("History fetched.", records));
    }

    public ResponseEntity<UserRecords.ApiResponse<UserRecords.PollDetailsRecord>> GetPollDetailsByRoomCode(String roomCode) {
        Optional<PollEntity> optionalPoll = findByRoomCode(roomCode);
        if (optionalPoll.isPresent()) {
            return ResponseEntity.ok(UserRecords.ApiResponse.success("Room found.", mapPollDetails(optionalPoll.get())));
        }

        String normalizedRoomCode = roomCode.trim().toUpperCase();
        Optional<PollSessionEntity> optionalSession = pollSessionRepository.findByRoomCode(normalizedRoomCode);
        if (optionalSession.isPresent()) {
            PollSessionEntity session = optionalSession.get();
            List<UserRecords.PollQuestionDetailsRecord> mappedQuestions = session.getQuestions().stream()
                    .map(question -> {
                        ensureResultCollections(question);
                        return new UserRecords.PollQuestionDetailsRecord(
                                question.getTitle(),
                                question.getType(),
                                question.getOptions(),
                                question.getMaxAnswers(),
                                question.getVotes(),
                                question.getAnswers()
                        );
                    })
                    .toList();
            return ResponseEntity.ok(UserRecords.ApiResponse.success("Archived room found.", new UserRecords.PollDetailsRecord(
                    "Beendete Umfrage (" + session.getRoomCode() + ")",
                    "FINISHED",
                    0,
                    mappedQuestions
            )));
        }

        return ResponseEntity.status(404).body(UserRecords.ApiResponse.error("Room not found.", null));
    }

    @Transactional
    public ResponseEntity<UserRecords.ApiResponse<UserRecords.QuestionResultsRecord>> SubmitVote(String roomCode, UserRecords.VoteRequestRecord voteRequest) {
        Optional<PollEntity> optionalPoll = findByRoomCode(roomCode);
        if (optionalPoll.isEmpty()) {
            return ResponseEntity.status(404).body(UserRecords.ApiResponse.error("Room not found.", null));
        }

        PollEntity poll = optionalPoll.get();
        Integer questionIndex = voteRequest.questionIndex();
        if (questionIndex == null || questionIndex < 0 || questionIndex >= poll.getQuestions().size()) {
            return ResponseEntity.badRequest().body(UserRecords.ApiResponse.error("Invalid question index.", null));
        }

        QuestionEntity question = poll.getQuestions().get(questionIndex);
        ensureResultCollections(question);

        String participantId = voteRequest.participantId();
        if (participantId == null || participantId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(UserRecords.ApiResponse.error("Participant ID is required.", null));
        }

        if (question.getVotedParticipants().contains(participantId)) {
            return ResponseEntity.status(409).body(UserRecords.ApiResponse.error("You have already voted on this question.", null));
        }

        if ("text".equals(question.getType())) {
            String answer = voteRequest.answer() == null ? "" : voteRequest.answer().trim();
            if (answer.isBlank()) {
                return ResponseEntity.badRequest().body(UserRecords.ApiResponse.error("Answer cannot be empty.", null));
            }
            question.getAnswers().add(answer);
        } else if ("multi".equals(question.getType())) {
            List<Integer> optionIndexes = selectedOptionIndexes(voteRequest);
            Integer maxAnswers = question.getMaxAnswers() == null ? 1 : question.getMaxAnswers();

            if (optionIndexes.isEmpty()) {
                return ResponseEntity.badRequest().body(UserRecords.ApiResponse.error("At least one option must be selected.", null));
            }

            if (optionIndexes.size() > maxAnswers) {
                return ResponseEntity.badRequest().body(UserRecords.ApiResponse.error("Too many options selected.", null));
            }

            for (Integer optionIndex : optionIndexes) {
                if (!isValidOptionIndex(question, optionIndex)) {
                    return ResponseEntity.badRequest().body(UserRecords.ApiResponse.error("Invalid option index.", null));
                }
            }

            for (Integer optionIndex : optionIndexes) {
                question.getVotes().set(optionIndex, question.getVotes().get(optionIndex) + 1);
            }
        } else {
            Integer optionIndex = voteRequest.optionIndex();
            if (!isValidOptionIndex(question, optionIndex)) {
                return ResponseEntity.badRequest().body(UserRecords.ApiResponse.error("Invalid option index.", null));
            }
            question.getVotes().set(optionIndex, question.getVotes().get(optionIndex) + 1);
        }

        question.getVotedParticipants().add(participantId);
        pollRepository.save(poll);

        UserRecords.QuestionResultsRecord results = mapQuestionResults(questionIndex, question);
        messagingTemplate.convertAndSend("/topic/poll/" + roomCode + "/results", results);
        return ResponseEntity.ok(UserRecords.ApiResponse.success("Answer submitted.", results));
    }

    @Transactional
    public boolean ApplyLiveCommand(String roomCode, UserRecords.LiveCommandRecord command, String hostEmail) {
        Optional<PollEntity> optionalPoll = findByRoomCode(roomCode);
        if (optionalPoll.isEmpty() || command == null || command.command() == null) {
            return false;
        }

        PollEntity poll = optionalPoll.get();
        if (!isPollOwner(poll, hostEmail)) {
            return false;
        }

        switch (command.command()) {
            case "START_POLL" -> {
                poll.setStatus("LIVE");
                poll.setActiveQuestionIndex(0);
            }
            case "NEXT_QUESTION" -> {
                poll.setStatus("LIVE");
                poll.setActiveQuestionIndex(validQuestionIndex(poll, command.questionIndex()));
            }
            case "END_POLL" -> {
                poll.setStatus("FINISHED");
                poll.setActiveQuestionIndex(validQuestionIndex(poll, command.questionIndex()));
            }
            default -> {
                return false;
            }
        }
        pollRepository.save(poll);
        return true;
    }

    private UserRecords.PollDetailsRecord mapPollDetails(PollEntity poll) {
        List<UserRecords.PollQuestionDetailsRecord> questions = poll.getQuestions().stream()
                .map(question -> {
                    ensureResultCollections(question);
                    return new UserRecords.PollQuestionDetailsRecord(
                            question.getTitle(),
                            question.getType(),
                            question.getOptions(),
                            question.getMaxAnswers(),
                            question.getVotes(),
                            question.getAnswers()
                    );
                })
                .toList();

        return new UserRecords.PollDetailsRecord(poll.getTitle(), poll.getStatus(), currentQuestionIndex(poll), questions);
    }

    private UserRecords.SavedPollRecord mapSavedPoll(PollEntity poll) {
        List<UserRecords.PollQuestionRecord> questions = poll.getQuestions().stream()
                .map(question -> new UserRecords.PollQuestionRecord(
                        question.getTitle(),
                        question.getType(),
                        question.getOptions(),
                        question.getMaxAnswers()
                ))
                .toList();

        return new UserRecords.SavedPollRecord(
                poll.getId(),
                poll.getTitle(),
                poll.getRoomCode(),
                poll.getStatus(),
                currentQuestionIndex(poll),
                questions.size(),
                questions
        );
    }

    private List<QuestionEntity> mapPollRequestQuestions(UserRecords.PollRecord pollRequest) {
        if (pollRequest.questions() == null) {
            return List.of();
        }

        return pollRequest.questions().stream()
                .map(record -> new QuestionEntity(
                        record.title(),
                        record.type(),
                        record.options() == null ? new ArrayList<>() : new ArrayList<>(record.options()),
                        record.maxAnswers()
                ))
                .toList();
    }

    private List<QuestionEntity> resetQuestions(List<QuestionEntity> questions) {
        return questions.stream()
                .map(question -> new QuestionEntity(
                        question.getTitle(),
                        question.getType(),
                        question.getOptions() == null ? new ArrayList<>() : new ArrayList<>(question.getOptions()),
                        question.getMaxAnswers()
                ))
                .toList();
    }

    private List<QuestionEntity> cloneQuestionsWithVotes(List<QuestionEntity> questions) {
        return questions.stream()
                .map(question -> {
                    QuestionEntity q = new QuestionEntity(
                            question.getTitle(),
                            question.getType(),
                            question.getOptions() == null ? new ArrayList<>() : new ArrayList<>(question.getOptions()),
                            question.getMaxAnswers()
                    );
                    q.setVotes(question.getVotes() == null ? new ArrayList<>() : new ArrayList<>(question.getVotes()));
                    q.setAnswers(question.getAnswers() == null ? new ArrayList<>() : new ArrayList<>(question.getAnswers()));
                    q.setVotedParticipants(question.getVotedParticipants() == null ? new java.util.HashSet<>() : new java.util.HashSet<>(question.getVotedParticipants()));
                    return q;
                })
                .toList();
    }

    private UserRecords.QuestionResultsRecord mapQuestionResults(Integer questionIndex, QuestionEntity question) {
        ensureResultCollections(question);
        return new UserRecords.QuestionResultsRecord(
                questionIndex,
                question.getType(),
                question.getVotes(),
                question.getAnswers()
        );
    }

    private void ensureResultCollections(QuestionEntity question) {
        if (question.getOptions() == null) {
            question.setOptions(new ArrayList<>());
        }

        if (question.getVotes() == null) {
            question.setVotes(new ArrayList<>());
        }

        while (question.getVotes().size() < question.getOptions().size()) {
            question.getVotes().add(0);
        }

        if (question.getAnswers() == null) {
            question.setAnswers(new ArrayList<>());
        }

        if (question.getVotedParticipants() == null) {
            question.setVotedParticipants(new java.util.HashSet<>());
        }
    }

    private Optional<PollEntity> findByRoomCode(String roomCode) {
        if (roomCode == null) {
            return Optional.empty();
        }

        String normalizedRoomCode = roomCode.trim().toUpperCase();
        if (!normalizedRoomCode.matches("[A-Z0-9]{6}")) {
            return Optional.empty();
        }

        return pollRepository.findByRoomCode(normalizedRoomCode);
    }

    private boolean isPollOwner(PollEntity poll, String hostEmail) {
        if (poll.getUserID() == null || hostEmail == null || hostEmail.isBlank()) {
            return false;
        }

        return userRepository.findByEmail(hostEmail)
                .map(user -> poll.getUserID().equals(user.getId()))
                .orElse(false);
    }

    private Integer currentQuestionIndex(PollEntity poll) {
        return validQuestionIndex(poll, poll.getActiveQuestionIndex());
    }

    private Integer validQuestionIndex(PollEntity poll, Integer requestedIndex) {
        if (poll.getQuestions() == null || poll.getQuestions().isEmpty()) {
            return 0;
        }

        if (requestedIndex == null) {
            return 0;
        }

        return Math.max(0, Math.min(requestedIndex, poll.getQuestions().size() - 1));
    }

    private List<Integer> selectedOptionIndexes(UserRecords.VoteRequestRecord voteRequest) {
        LinkedHashSet<Integer> optionIndexes = new LinkedHashSet<>();

        if (voteRequest.optionIndexes() != null) {
            optionIndexes.addAll(voteRequest.optionIndexes());
        }

        if (voteRequest.optionIndex() != null) {
            optionIndexes.add(voteRequest.optionIndex());
        }

        return new ArrayList<>(optionIndexes);
    }

    private boolean isValidOptionIndex(QuestionEntity question, Integer optionIndex) {
        return optionIndex != null && optionIndex >= 0 && optionIndex < question.getOptions().size();
    }

}
