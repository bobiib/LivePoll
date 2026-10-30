package com.livepoll.backendsrc.API.service.tokens;

import com.livepoll.backendsrc.API.entity.jpa.UserEntity;
import com.livepoll.backendsrc.API.exception.customExceptions.ExpiredTokenException;
import com.livepoll.backendsrc.API.exception.customExceptions.ParseTokenException;
import com.livepoll.backendsrc.API.repository.jpa.UserRepository;
import com.livepoll.backendsrc.Util.TimeUtil;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.Optional;

@Service
public class TokenService {

    private final UserRepository userRepository;

    public TokenService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Secret value configured via environment variable JWT_SECRET
    @Value("${jwt.secret}")
    private String tokenSecret;

    //Methods below are for generating JWTToken and RefreshToken
    public String generateJWTToken(UserEntity userEntity) throws JOSEException {

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(userEntity.getEmail())
                .issuer("https://buenows.org")
                .issueTime(Date.from(TimeUtil.getCurrentDate()))
                .expirationTime(Date.from(TimeUtil.getHourFromNow()))
                .build();

        byte[] sharedSecret = tokenSecret.getBytes(StandardCharsets.UTF_8);
        JWSSigner signer = new MACSigner(sharedSecret);
        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(new JWSHeader(JWSAlgorithm.HS256), payload);
        jwsObject.sign(signer);

        return jwsObject.serialize();
    }

    public JWTClaimsSet validateJWTToken(String token){
        JWTClaimsSet claimsSet;
        try {
            JWSObject jwsObject = JWSObject.parse(token);
            JWSVerifier verifier = new MACVerifier(tokenSecret.getBytes(StandardCharsets.UTF_8));
            if(!jwsObject.verify(verifier)){
                throw new ParseTokenException("Please Log in again.", "INVALID_TOKEN");
            }
            claimsSet = JWTClaimsSet.parse(jwsObject.getPayload().toJSONObject());
        }catch (JOSEException | ParseException e) {
            throw new ParseTokenException("Please Log in again.", "INVALID_TOKEN", e);
        }

        //Validating if Token is expired.
        Date currentDate = new Date(System.currentTimeMillis());
        if (currentDate.after(claimsSet.getExpirationTime())){
            throw new ExpiredTokenException("Please Log in again.", "EXPIRED_TOKEN");
        }

        return claimsSet;
    }

    //Methods below Parse Details about User from Token
    public String getUsernameFromToken(String token) throws ParseException {
        JWSObject jwsObject = JWSObject.parse(token);
        return jwsObject.getPayload().toJSONObject().get("sub").toString();
    }
    public Date getExpirationFromToken(String token) throws ParseException {
        JWSObject jwsObject = JWSObject.parse(token);
        JWTClaimsSet claimSet = JWTClaimsSet.parse(jwsObject.getPayload().toJSONObject());
        return claimSet.getExpirationTime();
    }
    public String parseTokenFromHeader(String header){
        String token = "";
        if(header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        }
        return token;
    }
}
