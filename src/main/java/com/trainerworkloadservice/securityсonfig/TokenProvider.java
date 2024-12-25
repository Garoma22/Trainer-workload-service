package com.trainerworkloadservice.securityсonfig;

import static java.util.stream.Collectors.toList;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import java.util.Collections;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class TokenProvider {

  @Value("${jwt.secret-key}")
  private String secretKey;

  public Claims getAllClaimsFromToken(String token) {
    JwtParser parser = Jwts.parserBuilder()
        .setSigningKey(secretKey)
        .build();

    return parser.parseClaimsJws(token).getBody();
  }


  public Authentication getAuthentication(String token) {
    Claims claims = getAllClaimsFromToken(token);
    List<String> roles = claims.get("roles", List.class);
    List<SimpleGrantedAuthority> authorities;

    if (roles != null) {
      authorities = roles.stream()
          .map(SimpleGrantedAuthority::new)
          .toList();
    } else {
      authorities = Collections.emptyList();
    }

    User principal = new User(claims.getSubject(), "", authorities);
    return new UsernamePasswordAuthenticationToken(principal, token, authorities);
  }

  public boolean isTokenExpired(String token) {
    return getAllClaimsFromToken(token).getExpiration().before(new Date());
  }

  public boolean validateToken(String token) {
    return !isTokenExpired(token);
  }
}