package com.unibuc.mobiliar.services;
import com.unibuc.mobiliar.entities.Customer;
import com.unibuc.mobiliar.repositories.CustomerRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;
@Service
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public CustomerService(CustomerRepository customerRepository, BCryptPasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }
    public Optional<Customer> findCustomerByEmail(String email) {
        return customerRepository.findByEmail(email);
    }
    public void saveCustomer(Customer customer) {
        customerRepository.save(customer);
    }

    //
    public boolean activateCustomer(Customer customer) {
        try {
            customer.setActivatedAccount(true);
            customerRepository.save(customer);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public boolean checkEmailExists(String email) {
        return customerRepository.existsByEmail(email);
    }
    public boolean checkPhoneExists(String phoneNumber) {
        return customerRepository.existsByPhoneNumber(phoneNumber);
    }

    public Optional<Customer> authenticate(String email, String password) {
        Optional<Customer> customer = findCustomerByEmail(email);
        if (customer.isPresent() && passwordEncoder.matches(password, customer.get().getPassword())) {
            return customer;
        } else {
            return Optional.empty();
        }
    }

    public boolean updatePassword(Customer customer, String newPassword) {
        try {
            customer.setPassword(passwordEncoder.encode(newPassword));
            saveCustomer(customer);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
