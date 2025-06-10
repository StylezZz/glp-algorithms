package com.glp.glpDP1.repository;

import com.glp.glpDP1.persistence.entity.BloqueoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BloqueoRepository extends JpaRepository<BloqueoEntity, String> {
}
