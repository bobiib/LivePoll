package com.livepoll.backendsrc.Util;

import com.livepoll.backendsrc.API.entity.jpa.UserEntity;
import com.livepoll.backendsrc.API.entity.mongo.PollEntity;
import com.livepoll.backendsrc.API.exception.customExceptions.InvalidPollRequestException;
import com.livepoll.backendsrc.API.exception.customExceptions.InvalidUserException;
import com.livepoll.backendsrc.API.repository.jpa.UserRepository;
import com.livepoll.backendsrc.API.repository.mongo.PollRepository;
import com.livepoll.backendsrc.API.service.tokens.TokenService;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class RetrievalUtil {

        private final TokenService tokenService;
        private final UserRepository userRepository;
        private final PollRepository pollRepository;

        public RetrievalUtil(TokenService tokenService, UserRepository userRepository, PollRepository pollRepository) {
            this.tokenService = tokenService;
            this.userRepository = userRepository;
            this.pollRepository = pollRepository;
        }

        public UserEntity getUserEntityFromToken(String token){
            com.nimbusds.jwt.JWTClaimsSet claimsSet = tokenService.validateJWTToken(token);
            Optional<UserEntity> userEntity = userRepository.findByEmail(claimsSet.getSubject());
            if(userEntity.isPresent()){
                return userEntity.get();
            }else {
                throw new InvalidUserException("User not found.", "INVALID_USER");
            }
        }

        public UserEntity getUserEntityFromEmail(String email){
            Optional<UserEntity> userEntity = userRepository.findByEmail(email);
            if(userEntity.isPresent()){
                return userEntity.get();
            }else {
                throw new InvalidUserException("User not found.", "INVALID_USER");
            }
        }

        public List<PollEntity> getPollEntityfromUser_id(UUID uuid){
            List<PollEntity> pollEntityList = pollRepository.findPollEntitiesByUserID(uuid);
            if (!pollEntityList.isEmpty()){
                return pollEntityList;
            }else {
                throw new InvalidPollRequestException("Could not get Polls for this User", "INVALID_POLL");
            }
        }
}
