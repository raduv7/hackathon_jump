package hackathon_jump.server.business.service.auth;

import hackathon_jump.server.model.dto.Session;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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

  public String issue(Session session) {
    Map<String, Object> claims = Map.of(
      "googleEmailAddresses", session.getGoogleEmailAddresses(),
      "facebookUsername", session.getFacebookUsername(),
      "linkedinUsername", session.getLinkedinUsername()
    );
    Instant now = Instant.now();
    return Jwts.builder()
            .issuer(issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(expiresMinutes, ChronoUnit.MINUTES)))
            .claims(claims)
            .signWith(key)
            .compact();
  }

  public String issue(String subject, Map<String, Object> claims) {
    Instant now = Instant.now();
    return Jwts.builder()
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

  public Claims validateAndGetClaims(String jwt) {
    return Jwts.parser()
        .verifyWith(key)
        .requireIssuer(issuer)
        .build()
        .parseSignedClaims(jwt)
        .getPayload();
  }

  public Session validateAndGetSession(String jwt) {
    Claims claims = validateAndGetClaims(jwt);
    List<String> googleEmailAddresses = (List<String>) claims.get("googleEmailAddresses");
    String facebookUsername = (String) claims.get("facebookUsername");
    String linkedinUsername = (String) claims.get("linkedinUsername");

    return new Session(googleEmailAddresses, facebookUsername, linkedinUsername);
  }

  public Session mergeSessions(Session session, Session other) {
    List<String> googleEmailAddresses = Stream.concat(
            session.getGoogleEmailAddresses().stream(),
            other.getGoogleEmailAddresses().stream())
            .distinct().toList();
    String facebookUsername = other.getFacebookUsername() == null ? session.getFacebookUsername() : other.getFacebookUsername();
    String linkedinUsername = other.getLinkedinUsername() == null ? session.getLinkedinUsername() : other.getLinkedinUsername();

    return new Session(googleEmailAddresses, facebookUsername, linkedinUsername);
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
