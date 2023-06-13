package com.unibuc.mobiliar.controllers;

import com.unibuc.mobiliar.dto.AccountExistsResponse;
import com.unibuc.mobiliar.entities.Customer;
import com.unibuc.mobiliar.services.EmailService;
import com.unibuc.mobiliar.services.JwtTokenService;
import com.unibuc.mobiliar.services.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.Optional;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final EmailService emailService;
    private final JwtTokenService jwtTokenService;

//    private static final Logger logger = LogManager.getLogger(CustomerController.class);
    private final CustomerService customerService;
    public CustomerController(EmailService emailService, JwtTokenService jwtTokenService, CustomerService customerService) {
        this.emailService = emailService;
        this.jwtTokenService = jwtTokenService;
        this.customerService = customerService;
    }
    @GetMapping("/check-account-exists")
    public ResponseEntity<AccountExistsResponse> checkAccountExists(@RequestParam("email") String email,
                                                                    @RequestParam("phoneNumber") String phoneNumber) {
        boolean emailExists = customerService.checkEmailExists(email);
        boolean phoneExists = customerService.checkPhoneExists(phoneNumber);
        AccountExistsResponse response = new AccountExistsResponse(emailExists, phoneExists);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/register")
    public ResponseEntity<?> registerCustomer(@RequestParam("name") String name,
                                                   @RequestParam("email") String email,
                                                   @RequestParam("password") String password,
                                                   @RequestParam("address1") String address1,
                                                   @RequestParam(value="address2", required = false) String address2,
                                                   @RequestParam("phoneNumber") String phoneNumber) {
        Customer customer = Customer.builder()
                .name(name)
                .email(email)
                .password(password)
                .address1(address1)
                .address2(address2)
                .phoneNumber(phoneNumber)
                .build();
//        customerService.saveCustomer(customer);
        return ResponseEntity.ok().body(customer);
    }
    @PostMapping("/send-activation-email")
    public ResponseEntity<String> sendActivationEmail(@RequestParam("email") String email,
                                                      @RequestParam("activationUrl") String activationUrl) {
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
        return new ResponseEntity<>("Activation email sent successfully", HttpStatus.OK);
    }
}