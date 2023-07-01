package com.unibuc.mobiliar.controllers;

import com.nimbusds.jose.JOSEException;
import com.unibuc.mobiliar.dto.AccountExistsResponse;
import com.unibuc.mobiliar.dto.LoginRequest;
import com.unibuc.mobiliar.entities.Customer;
import com.unibuc.mobiliar.services.EmailService;
import com.unibuc.mobiliar.services.JwtTokenService;
import com.unibuc.mobiliar.services.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class CustomerController {
    private final EmailService emailService;
    private final JwtTokenService jwtTokenService;
    private final CustomerService customerService;
    private final BCryptPasswordEncoder passwordEncoder;

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    public CustomerController(EmailService emailService, JwtTokenService jwtTokenService, CustomerService customerService, BCryptPasswordEncoder passwordEncoder)
    {
        this.emailService = emailService;
        this.jwtTokenService = jwtTokenService;
        this.customerService = customerService;
        this.passwordEncoder = passwordEncoder;
    }


    @GetMapping("/check-account-exists")
    public ResponseEntity<AccountExistsResponse> checkAccountExists(@RequestParam("email") String email,
                                                                    @RequestParam("phoneNumber") String phoneNumber)
    {
        logger.info("Checking if account exists with email: {} and phoneNumber: {}", email, phoneNumber);
        boolean emailExists = customerService.checkEmailExists(email);
        boolean phoneExists = customerService.checkPhoneExists(phoneNumber);
        AccountExistsResponse response = new AccountExistsResponse(emailExists, phoneExists);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/get-name-by-email")
    public ResponseEntity<?> getNameByEmail(@RequestParam("email") String email) {
        if (email.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email cannot be empty");
        }
        String name = customerService.getNameByEmail(email);
        if (name != null) {
            return ResponseEntity.ok(name);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Customer not found");
        }
    }


    @PostMapping("/is-account-activated")
    public ResponseEntity<?> isAccountActivated(@RequestParam String email) {
        Optional<Customer> customer = customerService.findCustomerByEmail(email);
        if (customer.isEmpty()) {
            return new ResponseEntity<>("Customer not found", HttpStatus.NOT_FOUND);
        } else {
            // Return the boolean value directly
            boolean isActivated = customer.get().isActivatedAccount();
            Map<String, Boolean> response = new HashMap<>();
            logger.info("Account activation status for email {}: {}", email, isActivated);
            response.put("activatedAccount", isActivated);
            return ResponseEntity.ok().body(response);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerCustomer(@RequestParam("name") String name,
                                              @RequestParam("email") String email,
                                              @RequestParam("password") String password,
                                              @RequestParam("address1") String address1,
                                              @RequestParam(value="address2", required = false) String address2,
                                              @RequestParam("phoneNumber") String phoneNumber)
    {
        logger.info("Registering a new customer with email: {}", email);
        String hashedPassword = passwordEncoder.encode(password);

        Customer customer = Customer.builder().name(name)
                .email(email)
                .password(hashedPassword)
                .address1(address1)
                .address2(address2)
                .phoneNumber(phoneNumber)
                .build();
        customerService.saveCustomer(customer);
        logger.info("Customer registered successfully with id: {}", customer.getId());
        return ResponseEntity.ok().body(customer);
    }   
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) throws JOSEException {
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();
        logger.info("Attempting to login with email: {}", email);
        Optional<Customer> customer = customerService.authenticate(email, password);
        if (customer.isEmpty()) {
            logger.warn("Invalid email or password for email: {}", email);
            return new ResponseEntity<>("Invalid email or password", HttpStatus.UNAUTHORIZED);
        } else {
            String token = jwtTokenService.generateToken(customer.get().getEmail());
            logger.info("Login successful, token generated for email: {}", email);
            return ResponseEntity.ok().body(Collections.singletonMap("token", token));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestHeader("Authorization") String bearerRefreshToken) throws JOSEException {
        logger.info("Attempting to refresh token");
        // Extract the token from the "Bearer" prefix in the header
        if (bearerRefreshToken == null || !bearerRefreshToken.startsWith("Bearer ")) {
            return new ResponseEntity<>("Invalid refresh token format", HttpStatus.BAD_REQUEST);
        }
        String refreshToken = bearerRefreshToken.substring(7);

        String email;
        try {
            email = jwtTokenService.verifyToken(refreshToken);
        } catch (Exception e) {
            logger.error("Error verifying refresh token: {}", e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        Optional<Customer> customer = customerService.findCustomerByEmail(email);
        if (customer.isEmpty()) {
            return new ResponseEntity<>("Customer not found", HttpStatus.NOT_FOUND);
        } else {
            String newToken = jwtTokenService.generateToken(customer.get().getEmail());
            logger.info("Token refreshed for email: {}", email);
            return ResponseEntity.ok().body(Collections.singletonMap("token", newToken));
        }
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUser(@RequestHeader("Authorization") String bearerToken) throws JOSEException {
        logger.info("Retrieving user details for token: {}", bearerToken);

        // Extract the token from the "Bearer" prefix in the header
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            return new ResponseEntity<>("Invalid token format", HttpStatus.BAD_REQUEST);
        }
        String token = bearerToken.substring(7);

        String email;
        try {
            email = jwtTokenService.verifyToken(token);
        } catch (Exception e) {
            logger.error("Error verifying token: {}", e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        Optional<Customer> customer = customerService.findCustomerByEmail(email);
        if (customer.isEmpty()) {
            return new ResponseEntity<>("Customer not found", HttpStatus.NOT_FOUND);
        } else {
            logger.info("User details retrieved for email: {}", email);
            return ResponseEntity.ok().body(customer.get());
        }
    }

    @PostMapping("/send-activation-email")
    public ResponseEntity<String> sendActivationEmail(@RequestParam("email") String email,
                                                      @RequestParam("activationUrl") String activationUrl)
    {
        logger.info("Sending activation email to: {}", email);
        Optional<Customer> customer = customerService.findCustomerByEmail(email);
        if (customer.isEmpty()) {
            return new ResponseEntity<>("Customer not found", HttpStatus.NOT_FOUND);
        }
        String token;
        try {
            token = jwtTokenService.generateToken(customer.get().getEmail());
        } catch (Exception e) {
            return new ResponseEntity<>("Error generating confirmation token", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        String emailSubject = "Mobiliar: Account Confirmation";
        String emailBody = "Hello, " + customer.get().getName() + "!\n\n" +
                "Please confirm your account by clicking the following link within 10 minutes: \n" +
                activationUrl + "?token=" + token;
        emailService.sendEmail(customer.get().getEmail(), emailSubject, emailBody);
        logger.info("Activation email sent successfully to: {}", email);
        return new ResponseEntity<>("Activation email sent successfully", HttpStatus.OK);
    }
    @PostMapping("/activate-account")
    public ResponseEntity<String> activateAccount(@RequestParam("token") String token) {
        logger.info("Activating account with token: {}", token);
        String email;
        try {
            email = jwtTokenService.verifyToken(token);
        } catch (Exception e) {
            logger.error("Error verifying token: {}", e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
            Optional<Customer> customer = customerService.findCustomerByEmail(email);
        if (customer.isEmpty()) {
            return new ResponseEntity<>("Customer not found", HttpStatus.NOT_FOUND);
        }

        if (customerService.activateCustomer(customer.get())) {
            logger.info("Account activated successfully for email: {}", email);
            return new ResponseEntity<>("Account activated successfully", HttpStatus.OK);
        } else {
            logger.error("Error activating account for email: {}", email);
            return new ResponseEntity<>("Error activating account", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/validate-token")
    public ResponseEntity<String> validateToken(@RequestParam("token") String token) {
        logger.info("Validating token: {}", token);
        String email;
        try {
            email = jwtTokenService.verifyToken(token);
        } catch (Exception e) {
            logger.error("Error verifying token: {}");
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        Optional<Customer> customer = customerService.findCustomerByEmail(email);
        if (customer.isEmpty()) {
            return new ResponseEntity<>("Customer not found", HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>("Token is valid", HttpStatus.OK);
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestParam("token") String token,
                                                 @RequestParam("newPassword") String newPassword) {
        logger.info("Changing password with token: {}", token);
        String email;
        try {
            email = jwtTokenService.verifyToken(token);
        } catch (Exception e) {
            logger.error("Error verifying token: {}", e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        Optional<Customer> customer = customerService.findCustomerByEmail(email);
        if (customer.isEmpty()) {
            return new ResponseEntity<>("Customer not found", HttpStatus.NOT_FOUND);
        }

        if (customerService.updatePassword(customer.get(), newPassword)) {
            logger.info("Password changed successfully for email: {}", email);
            return new ResponseEntity<>("Password changed successfully", HttpStatus.OK);
        } else {
            logger.error("Error changing password for email: {}", email);
            return new ResponseEntity<>("Error changing password", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam("email") String email,
                                                @RequestParam("resetUrl") String resetUrl) {
        logger.info("Resetting password for email: {}", email);
        Optional<Customer> customer = customerService.findCustomerByEmail(email);
        if (customer.isEmpty()) {
            return new ResponseEntity<>("Customer not found", HttpStatus.NOT_FOUND);
        }
        String token;
        try {
            token = jwtTokenService.generateToken(customer.get().getEmail());
        } catch (Exception e) {
            return new ResponseEntity<>("Error generating reset token", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        String emailSubject = "Mobiliar: Password Reset";
        String emailBody = "Hello, " + customer.get().getName() + "!\n\n" +
                "You requested a password reset. Please click the following link within 10 minutes to reset your password: \n" +
                resetUrl + "?token=" + token;
        emailService.sendEmail(customer.get().getEmail(), emailSubject, emailBody);
        logger.info("Password reset email sent successfully to: {}", email);
        return new ResponseEntity<>("Password reset email sent successfully", HttpStatus.OK);
    }
}