package com.carbon.api.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        logger.debug("Processing request: " + path);
        
        if (path.endsWith(".css") || 
            path.endsWith(".js") || 
            path.endsWith(".ico") || 
            path.startsWith("/css/") || 
            path.startsWith("/js/") || 
            path.startsWith("/images/")) {
            logger.debug("Skipping authentication for static resource: " + path);
            filterChain.doFilter(request, response);
            return;
        }
        
        String token = getJwtFromRequest(request);
        
        if (token != null) {
            logger.debug("Token found: " + token.substring(0, Math.min(10, token.length())) + "...");
            
            boolean isValid = jwtTokenProvider.validateToken(token);
            logger.debug("Token validity: " + isValid);
            
            if (isValid) {
                Authentication auth = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
                logger.debug("User '" + auth.getName() + "' authenticated successfully");
            } else {
                logger.warn("Provided token is invalid");
            }
        } else {
            logger.debug("No token found in request");
        }
        
        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        logger.debug("Authorization header: " + (bearerToken != null ? bearerToken.substring(0, Math.min(20, bearerToken.length())) + "..." : "null"));
        
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
} 