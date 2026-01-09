//package com.lms.ontocriptIT.backend.services.serviceImpl;
//
//import com.lms.ontocriptIT.backend.entity.ClassAccess;
//import com.lms.ontocriptIT.backend.repository.ClassAccessRepository;
//import com.lms.ontocriptIT.backend.services.centralServices.StudentService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//
//import java.util.Optional;
//
//@Service
//@RequiredArgsConstructor
//public class StudentServiceImpl implements StudentService {
//
//    private ClassAccessRepository classAccessRepository;
//    @Override
//    public ResponseEntity<?> getGrantZoomClass(Long studentId) {
//        Optional<ClassAccess> classAccess = classAccessRepository.findByStudentId(studentId);
//    }
//}
