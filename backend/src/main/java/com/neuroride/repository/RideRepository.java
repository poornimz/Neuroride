package com.neuroride.repository;
import com.neuroride.domain.entity.Ride;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface RideRepository extends JpaRepository<Ride, UUID> {}