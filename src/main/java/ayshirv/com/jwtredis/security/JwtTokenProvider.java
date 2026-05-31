package ayshirv.com.jwtredis.security;

import ayshirv.com.jwtredis.dto.TokenPair;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${jwt.refreshExpiration}")
    private long refreshTokenExpirationMs;

    // Generate JWT Token
    public TokenPair generateTokenPair(String username) {
        String accessToken = generateAccessToken(username);
        String refreshToken = generateRefreshToken(username);
        return new TokenPair(accessToken, refreshToken);
    }

    // 1. Access Token
    private String generateAccessToken(String username) {
        return generateToken(username, jwtExpirationMs,null);
    }

    // 2. Refresh Token
    private String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("scopes","refresh");
        return generateToken(username, refreshTokenExpirationMs,claims);
    }

    private String generateToken(String username,long expiry,Map<String, Object> claims) {
        // Calculate the expiration date
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiry);
        JwtBuilder builder = Jwts.builder()
                .issuer(issuer)
                .subject(username)
                .expiration(expiryDate)
                .signWith(getSigninKey());
        if (claims != null) {
            builder.claims(claims);
        }
        return builder.compact();
    }

    private SecretKey getSigninKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigninKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        }
        catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        }
        catch (ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
        }
        catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
