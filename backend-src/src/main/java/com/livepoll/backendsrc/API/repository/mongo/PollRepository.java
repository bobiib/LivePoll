package com.livepoll.backendsrc.API.repository.mongo;

import com.livepoll.backendsrc.API.entity.mongo.PollEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PollRepository extends MongoRepository<PollEntity, String> {
    List<PollEntity> findPollEntitiesByUserID(UUID uuid);
    Optional<PollEntity> findByRoomCode(String roomCode);
}
