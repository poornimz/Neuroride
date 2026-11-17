package com.neuroride.repository;

import com.neuroride.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {}