package com.unibuc.mobiliar.services;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;
import org.springframework.stereotype.Service;
import java.text.ParseException;
import java.util.Date;
@Service
public class JwtTokenService {
    private static final String SECRET_KEY = "your-secret-key-with-a-minimum-length-of-32-bytes";
    private static final long TOKEN_EXPIRATION_TIME_MS = 10 * 60 * 1000L; // 10 minutes
    public String generateToken(String email) throws JOSEException {
        JWSSigner signer;
        try {
            signer = new MACSigner(SECRET_KEY.getBytes());
        } catch (JOSEException e) {
            e.printStackTrace();
            throw e; // rethrow the exception if necessary
        }

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(email)
                .expirationTime(new Date(System.currentTimeMillis() + TOKEN_EXPIRATION_TIME_MS))
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
        signedJWT.sign(signer);

        return signedJWT.serialize();
    }
    public boolean isTokenValid(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(SECRET_KEY.getBytes());
            return signedJWT.verify(verifier) && !isTokenExpired(signedJWT);
        } catch (Exception e) {
            return false;
        }
    }
    private boolean isTokenExpired(SignedJWT signedJWT) throws ParseException {
        Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        return expirationTime.before(new Date());
    }
}