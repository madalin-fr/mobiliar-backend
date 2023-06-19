package com.unibuc.mobiliar.services;
import com.unibuc.mobiliar.entities.Furniture;
import com.unibuc.mobiliar.repositories.FurnitureRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
@Service
public class FurnitureService {
    private final FurnitureRepository furnitureRepository;
    public FurnitureService(FurnitureRepository furnitureRepository) {
        this.furnitureRepository = furnitureRepository;
    }
    public List<Furniture> getAllFurniture() {
        return furnitureRepository.findAll();
    }
    public Optional<Furniture> getFurnitureById(Long id) {
        return furnitureRepository.findById(id);
    }
    public void saveFurniture(Furniture furniture) {
        furnitureRepository.save(furniture);
    }

    public void removeFurniture(Furniture furniture) {
        furnitureRepository.delete(furniture);
    }
}