package com.neuroride.repository;
import com.neuroride.domain.entity.DriverProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DriverProfileRepository extends JpaRepository<DriverProfile, UUID> {}