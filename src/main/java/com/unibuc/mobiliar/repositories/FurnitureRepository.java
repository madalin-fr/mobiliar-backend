package com.unibuc.mobiliar.repositories;

import com.unibuc.mobiliar.entities.Furniture;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FurnitureRepository extends JpaRepository<Furniture, Long> {
}