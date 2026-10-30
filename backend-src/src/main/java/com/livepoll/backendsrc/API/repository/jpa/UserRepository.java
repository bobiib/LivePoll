package com.livepoll.backendsrc.API.repository.jpa;
import com.livepoll.backendsrc.API.entity.jpa.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository< UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findById(UUID id);
}
