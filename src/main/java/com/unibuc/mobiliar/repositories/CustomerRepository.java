package com.unibuc.mobiliar.repositories;
import com.unibuc.mobiliar.entities.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.config.web.servlet.oauth2.resourceserver.OpaqueTokenDsl;
import java.util.Optional;
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);

    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
}