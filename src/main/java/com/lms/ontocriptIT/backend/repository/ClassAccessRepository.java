package com.lms.ontocriptIT.backend.repository;


import com.lms.ontocriptIT.backend.entity.ClassAccess;
import com.lms.ontocriptIT.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassAccessRepository extends JpaRepository<ClassAccess, Long> {

    List<ClassAccess> findByStudent(User student);

    List<ClassAccess> findByStudentAndHasAccess(User student, boolean hasAccess);

    Optional<ClassAccess> findByStudentAndClassName(User student, String className);

    @Query("SELECT ca FROM ClassAccess ca WHERE ca.student.id = :studentId")
    List<ClassAccess> findByStudentId(Long studentId);

    @Query("SELECT ca FROM ClassAccess ca WHERE ca.student.id = :studentId AND ca.hasAccess = true")
    List<ClassAccess> findActiveAccessesByStudentId(Long studentId);

    @Query("SELECT CASE WHEN COUNT(ca) > 0 THEN true ELSE false END FROM ClassAccess ca WHERE ca.student.id = :studentId AND ca.hasAccess = true")
    boolean hasAnyActiveAccess(Long studentId);

    int countByStudentId(Long id);


    List<ClassAccess> findByStudentIdAndHasAccessTrue(Long studentId);
}

