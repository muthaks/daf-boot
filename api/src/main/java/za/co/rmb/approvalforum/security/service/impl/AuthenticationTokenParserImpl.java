package za.co.rmb.approvalforum.security.service.impl;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.InvalidClaimException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforum.security.model.AuthenticationTokenDetails;
import za.co.rmb.approvalforum.security.service.AuthenticationTokenParser;
import za.co.rmb.approvalforum.security.service.AuthenticationTokenSettings;
import jakarta.xml.bind.DatatypeConverter;

public class AuthenticationTokenParserImpl implements AuthenticationTokenParser {

    private AuthenticationTokenSettings settings;
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationTokenParserImpl.class);

    @Inject
    public AuthenticationTokenParserImpl(AuthenticationTokenSettings settings) {
        this.settings = settings;
    }

    @Override
    public AuthenticationTokenDetails parseToken(String token) {

        try {
            byte[] bytes = Base64.getDecoder().decode(settings.getSecret().getBytes());
            SecretKey key = Keys.hmacShaKeyFor(bytes);

//            String[] parts = token.split("\\.");
//            String header = (decode(parts[0]));
//            String payload = (decode(parts[1]));
//            String signature = (decode(parts[2]));
//
//            LOGGER.info("*** PARSER Token HEADER " + header);
//            LOGGER.info("*** PARSER Token PAYLOAD " + payload);
//            LOGGER.info("*** PARSER Token SIGNATURE " + signature);
//
//            LOGGER.info("######### PARSER SECRET " + settings.getSecret());
//            LOGGER.info("######### PARSER KEY " + DatatypeConverter.printHexBinary(key.getEncoded()));

            Claims claims = Jwts.parser()
                    .setSigningKey(key)
                    .requireAudience(settings.getAudience())
                    .setAllowedClockSkewSeconds(settings.getClockSkew())
                    .parseClaimsJws(token)
                    .getBody();

            return new AuthenticationTokenDetails.Builder()
                    .withId(extractTokenIdFromClaims(claims))
                    .withUsername(extractUsernameFromClaims(claims))
                    .withRoles(extractRolesFromClaims(claims))
                    .withIssuedDate(extractIssuedDateFromClaims(claims))
                    .withExpirationDate(extractExpirationDateFromClaims(claims))
                    .withRefreshCount(extractRefreshCountFromClaims(claims))
                    .withRefreshLimit(extractRefreshLimitFromClaims(claims))
                    .build();

        } catch (UnsupportedJwtException | MalformedJwtException | IllegalArgumentException e) {
            throw new NotAuthorizedException("Invalid token 1", e);
        } catch (ExpiredJwtException e) {
            throw new NotAuthorizedException("Expired token", e);
        } catch (InvalidClaimException e) {
            throw new NotAuthorizedException("Invalid claims", e);
        } catch (Exception e) {
            throw new NotAuthorizedException("Invalid token 2", e);
        }
    }

    private static String decode(String encodedString) {
        return new String(Base64.getUrlDecoder().decode(encodedString));
    }

    @Override
    public AuthenticationTokenDetails parseSsoToken(String token) {

        try {
            Claims claims = Jwts.parser().setSigningKey(getKey(settings.getSsoPublicKey()))
                    .requireAudience(settings.getSsoAudience()).setAllowedClockSkewSeconds(settings.getClockSkew())
                    .parseClaimsJws(token).getBody();

            return new AuthenticationTokenDetails.Builder().withId(extractTokenIdFromClaims(claims))
                    .withUsername(extractUsernameFromClaims(claims))
                    .withRoles(extractSsoRolesOfFromClaims(claims))
                    .withIssuedDate(extractIssuedDateFromClaims(claims))
                    .withExpirationDate(extractExpirationDateFromClaims(claims))
                    // .withRefreshCount(extractRefreshCountFromClaims(claims))
                    // .withRefreshLimit(extractRefreshLimitFromClaims(claims))
                    .build();

        } catch (UnsupportedJwtException | MalformedJwtException | IllegalArgumentException e) {
            throw new NotAuthorizedException("Invalid token 1", e);
        } catch (ExpiredJwtException e) {
            throw new NotAuthorizedException("Expired token", e);
        } catch (InvalidClaimException e) {
            throw new NotAuthorizedException("Invalid claims", e);
        } catch (Exception e) {
            throw new NotAuthorizedException("Invalid token 2", e);
        }
    }

    /**
     * Extract the token identifier from the token claims.
     *
     * @param claims
     * @return Identifier of the JWT token
     */
    private String extractTokenIdFromClaims(Claims claims) {
        return (String) claims.get(Claims.ID);
    }

    /**
     * Extract the username from the token claims.
     *
     * @param claims
     * @return Username from the JWT token
     */
    private String extractUsernameFromClaims(Claims claims) {
        return claims.getSubject();
    }

    /**
     * Extract the user roles from the token claims.
     *
     * @param claims
     * @return User roles from the JWT token
     */
    private Set<String> extractRolesFromClaims(Claims claims) {
        List<String> rolesAsString = (List<String>) claims.getOrDefault("roles", new ArrayList<>());
        return rolesAsString.stream().collect(Collectors.toSet());
    }

     /**
     * Extract the user roles from the token claims for SSO.
     *
     * @param claims
     * @return User roles from the JWT token for SSO.
     */
    private Set<String> extractSsoRolesOfFromClaims(Claims claims) {        
        List<String> rolesAsString = new ArrayList<>();
        String resourceAccess = claims.get("resource_access").toString();

        String roles = resourceAccess.substring(resourceAccess.indexOf(settings.getSsoClientId() + "={roles=[") + (settings.getSsoClientId() + "={roles=[").length());
        roles = roles.substring(0, roles.indexOf("]"));
         
        rolesAsString.addAll(Arrays.asList(roles.split(",")));
        return rolesAsString.stream().collect(Collectors.toSet());
    }

    /**
     * Extract the issued date from the token claims.
     *
     * @param claims
     * @return Issued date of the JWT token
     */
    private ZonedDateTime extractIssuedDateFromClaims(Claims claims) {
        return ZonedDateTime.ofInstant(claims.getIssuedAt().toInstant(), ZoneId.systemDefault());
    }

    /**
     * Extract the expiration date from the token claims.
     *
     * @param claims
     * @return Expiration date of the JWT token
     */
    private ZonedDateTime extractExpirationDateFromClaims(Claims claims) {
        return ZonedDateTime.ofInstant(claims.getExpiration().toInstant(), ZoneId.systemDefault());
    }

    /**
     * Extract the refresh count from the token claims.
     *
     * @param claims
     * @return Refresh count from the JWT token
     */
    private int extractRefreshCountFromClaims(Claims claims) {
        return (int) claims.get(settings.getRefreshCountClaimName());
    }

    /**
     * Extract the refresh limit from the token claims.
     *
     * @param claims
     * @return Refresh limit from the JWT token
     */
    private int extractRefreshLimitFromClaims(Claims claims) {
        return (int) claims.get(settings.getRefreshLimitClaimName());
    }

    private PublicKey getKey(String key){
        try{
            byte[] byteKey = Base64.getDecoder().decode(key.getBytes());
            X509EncodedKeySpec X509publicKey = new X509EncodedKeySpec(byteKey);
            KeyFactory kf = KeyFactory.getInstance("RSA");
    
            return kf.generatePublic(X509publicKey);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    
        return null;
    }

}
