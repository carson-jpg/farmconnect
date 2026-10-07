package com.farmconnect.repository;

import com.farmconnect.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FarmerGroupRepository extends JpaRepository<FarmerGroup, Long> {
    List<FarmerGroup> findAllByOrderByNameAsc();
}
