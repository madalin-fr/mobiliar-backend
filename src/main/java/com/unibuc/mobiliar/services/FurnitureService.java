package com.unibuc.mobiliar.services;

import com.unibuc.mobiliar.entities.Furniture;
import com.unibuc.mobiliar.repositories.FurnitureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FurnitureService {

    @Autowired
    private FurnitureRepository furnitureRepository;

    public List<Furniture> getAllFurniture() {
        return furnitureRepository.findAll();
    }

    public Optional<Furniture> getFurnitureById(Long id) {
        return furnitureRepository.findById(id);
    }

    public void saveFurniture(Furniture furniture) {
        furnitureRepository.save(furniture);
    }
}