package hackathon_jump.server.business.service.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final SecretKey key;
  private final String issuer;
  private final long expiresMinutes;

  public JwtService(
      @Value("${app.security.jwt.secret}") String secret,
      @Value("${app.security.jwt.issuer}") String issuer,
      @Value("${app.security.jwt.expires-minutes}") long expiresMinutes) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.issuer = issuer;
    this.expiresMinutes = expiresMinutes;
  }

  public String issue(String subject, Map<String, Object> claims) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(subject)
        .issuer(issuer)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(expiresMinutes, ChronoUnit.MINUTES)))
        .claims(claims)
        .signWith(key)
        .compact();
  }

  public String validateAndGetSubject(String jwt) {
    return Jwts.parser()
        .verifyWith(key)
        .requireIssuer(issuer)
        .build()
        .parseSignedClaims(jwt)
        .getPayload()
        .getSubject();
  }

  public boolean isTokenValid(String jwt, org.springframework.security.core.userdetails.UserDetails userDetails) {
    try {
      final String username = validateAndGetSubject(jwt);
      return (username.equals(userDetails.getUsername()) && !isTokenExpired(jwt));
    } catch (Exception e) {
      return false;
    }
  }

  private boolean isTokenExpired(String jwt) {
    try {
      return Jwts.parser()
          .verifyWith(key)
          .requireIssuer(issuer)
          .build()
          .parseSignedClaims(jwt)
          .getPayload()
          .getExpiration()
          .before(new Date());
    } catch (Exception e) {
      return true;
    }
  }
}
