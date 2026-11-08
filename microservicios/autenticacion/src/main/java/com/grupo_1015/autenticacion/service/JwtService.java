package com.grupo_1015.autenticacion.service;

import com.grupo_1015.autenticacion.model.Credencial;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

// Servicio: crea y verifica los tokens JWT.
// Un JWT es un texto firmado con nuestra clave secreta que lleva datos (correo, rol, plan)
// y una fecha de expiracion. Si alguien lo modifica, la firma ya no coincide.
@Service
public class JwtService {

    private final SecretKey clave;
    private final long expiracionMinutos;

    // La clave y la duracion vienen de application.properties (que las lee del .env)
    public JwtService(@Value("${jwt.secreto}") String secreto,
                      @Value("${jwt.expiracion-minutos}") long expiracionMinutos) {
        this.clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secreto));
        this.expiracionMinutos = expiracionMinutos;
    }

    public String generarToken(Credencial credencial) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(credencial.getCorreo())                 // a quien pertenece el token
                .claim("usuarioId", credencial.getUsuarioId())
                .claim("rol", credencial.getRol().name())
                .claim("plan", credencial.getPlan().name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expiracionMinutos, ChronoUnit.MINUTES)))
                .signWith(clave)                                 // firma con la clave secreta
                .compact();
    }

    // Verifica la firma y la expiracion, y devuelve el correo guardado en el token.
    // Si el token es falso, fue modificado o ya vencio, lanza una JwtException.
    public String obtenerCorreo(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public long getExpiracionMinutos() {
        return expiracionMinutos;
    }
}
