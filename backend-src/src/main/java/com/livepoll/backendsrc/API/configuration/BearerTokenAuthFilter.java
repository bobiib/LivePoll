package com.livepoll.backendsrc.API.configuration;

import com.livepoll.backendsrc.API.entity.jpa.UserEntity;
import com.livepoll.backendsrc.API.exception.customExceptions.ExpiredTokenException;
import com.livepoll.backendsrc.API.exception.customExceptions.ParseTokenException;
import com.livepoll.backendsrc.API.service.tokens.TokenService;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class BearerTokenAuthFilter extends OncePerRequestFilter {

    TokenService tokenService;
    HandlerExceptionResolver handlerExceptionResolver;

    public BearerTokenAuthFilter(TokenService tokenService, HandlerExceptionResolver handlerExceptionResolver) {
        this.tokenService = tokenService;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = tokenService.parseTokenFromHeader(request.getHeader("Authorization"));

        if (accessToken != null && !accessToken.isBlank()) {
            try {
                com.nimbusds.jwt.JWTClaimsSet claimsSet = tokenService.validateJWTToken(accessToken);
                String email = claimsSet.getSubject();



                Authentication authenticationToken =
                        new UsernamePasswordAuthenticationToken(email, null, null);

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

                handlerExceptionResolver.resolveException(request, response, null, e);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
