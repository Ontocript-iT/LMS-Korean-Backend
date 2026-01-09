package com.lms.ontocriptIT.backend.repository;

import com.lms.ontocriptIT.backend.entity.ZoomClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ZoomClassRepository extends JpaRepository<ZoomClass, Long> {
    List<ZoomClass> findByIsActiveTrueOrderByClassDateAsc();
}
