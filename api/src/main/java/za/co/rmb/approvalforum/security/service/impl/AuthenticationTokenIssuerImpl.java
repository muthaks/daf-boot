package za.co.rmb.approvalforum.security.service.impl;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import za.co.rmb.approvalforum.security.model.AuthenticationTokenDetails;
import za.co.rmb.approvalforum.security.service.AuthenticationTokenIssuer;
import za.co.rmb.approvalforum.security.service.AuthenticationTokenSettings;

import jakarta.inject.Inject;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Base64;

public class AuthenticationTokenIssuerImpl implements AuthenticationTokenIssuer {

    @Inject
    private AuthenticationTokenSettings settings;

    @Override
    public String issueToken(AuthenticationTokenDetails authenticationTokenDetails) {
        byte[] bytes = Base64.getDecoder().decode(settings.getSecret().getBytes());
        SecretKey key = Keys.hmacShaKeyFor(bytes);

        return Jwts.builder()
                .setId(authenticationTokenDetails.getId())
                .setIssuer(settings.getIssuer())
                .setAudience(settings.getAudience())
                .setSubject(authenticationTokenDetails.getUsername())
                .setIssuedAt(Date.from(authenticationTokenDetails.getIssuedDate().toInstant()))
                .setExpiration(Date.from(authenticationTokenDetails.getExpirationDate().toInstant()))
                .claim("roles", authenticationTokenDetails.getRoles())
                .claim(settings.getRefreshCountClaimName(), authenticationTokenDetails.getRefreshCount())
                .claim(settings.getRefreshLimitClaimName(), authenticationTokenDetails.getRefreshLimit())
                .signWith(key)
                .compact();
    }

}
