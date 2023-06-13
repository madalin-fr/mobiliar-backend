package com.unibuc.mobiliar.services;
import com.unibuc.mobiliar.entities.Customer;
import com.unibuc.mobiliar.repositories.CustomerRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;
@Service
public class CustomerService {
    private final CustomerRepository customerRepository;
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }
    public Optional<Customer> findCustomerByEmail(String email) {
        return customerRepository.findCustomerByEmail(email);
    }
    public void saveCustomer(Customer customer) {
        customerRepository.save(customer);
    }
    public boolean checkEmailExists(String email) {
        return customerRepository.existsByEmail(email);
    }
    public boolean checkPhoneExists(String phoneNumber) {
        return customerRepository.existsByPhoneNumber(phoneNumber);
    }
}
