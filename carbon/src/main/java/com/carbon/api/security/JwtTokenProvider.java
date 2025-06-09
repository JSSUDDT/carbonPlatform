package com.carbon.api.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationInMs;

    private Key key;

    @PostConstruct
    public void init() {
        logger.info("Initializing JWT provider, secret key length: {}", jwtSecret.length());
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate JWT token
     * @param vendorId Vendor ID
     * @param role Role, such as "ROLE_ADMIN" or "ROLE_USER"
     * @return JWT token
     */
    public String generateToken(String vendorId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);
        
        logger.debug("Generating token for user {}, role: {}, expiry time: {}", vendorId, role, expiryDate);

        String token = Jwts.builder()
                .setSubject(vendorId)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .claim("roles", role)
                .signWith(key)
                .compact();
        
        logger.debug("Token generated, length: {}", token.length());
        return token;
    }
    
    /**
     * Backward compatible token generation method
     * @param vendorId Vendor ID
     * @return JWT token, default role is "ROLE_USER"
     */
    public String generateToken(String vendorId) {
        return generateToken(vendorId, "ROLE_USER");
    }

    public String getVendorIdFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            String vendorId = claims.getSubject();
            logger.debug("User ID retrieved from token: {}", vendorId);
            return vendorId;
        } catch (Exception e) {
            logger.error("Unable to get user ID from token: {}", e.getMessage());
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            logger.debug("Validating token: {}", token.substring(0, Math.min(10, token.length())) + "...");
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            
            logger.debug("Token validation successful");
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("JWT validation error: {}", e.getMessage());
        }
        
        return false;
    }

    public Authentication getAuthentication(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String username = claims.getSubject();
            String roles = claims.get("roles", String.class);
            logger.debug("Getting authentication - user: {}, roles: {}", username, roles);

            List<SimpleGrantedAuthority> authorities = Arrays.asList(roles.split(","))
                    .stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            return new UsernamePasswordAuthenticationToken(username, "", authorities);
        } catch (Exception e) {
            logger.error("Error while getting authentication: {}", e.getMessage());
            return null;
        }
    }
} 