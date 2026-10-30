package com.livepoll.backendsrc.API.repository.mongo;

import com.livepoll.backendsrc.API.entity.mongo.PollSessionEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PollSessionRepository extends MongoRepository<PollSessionEntity, String> {
    List<PollSessionEntity> findByPollIdOrderByCreatedAtDesc(String pollId);
    Optional<PollSessionEntity> findByRoomCode(String roomCode);
}
