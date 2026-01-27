package com.lms.ontocriptIT.backend.controller;

import com.lms.ontocriptIT.backend.entity.ZoomClass;
import com.lms.ontocriptIT.backend.repository.ClassAccessRepository;
import com.lms.ontocriptIT.backend.repository.ZoomClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/admin/zoom-classes")
@RequiredArgsConstructor
public class ZoomClassController {
    private final ZoomClassRepository zoomClassRepository;

    private final ClassAccessRepository classAccessRepository;

    @PostMapping("/add")
    public ResponseEntity<?> addZoomClass(@RequestBody ZoomClass zoomClass) {
        try{
            HashMap<String,Object> response = new HashMap<>();
            response.put("message", "Zoom class added successfully");
            response.put("status", HttpStatus.CREATED.value());
            response.put("zoomClass", zoomClassRepository.save(zoomClass));
            return ResponseEntity.ok(response);
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error adding Zoom class: " + e.getMessage());
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateClass(@PathVariable Long id, @RequestBody ZoomClass updatedClass) {
        try{
            ZoomClass existingClass = zoomClassRepository.findById(id).orElse(null);
            if(existingClass == null){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Zoom class not found with id: " + id);
            }
            existingClass.setTitle(updatedClass.getTitle());
            existingClass.setNote(updatedClass.getNote());
            existingClass.setClassDate(updatedClass.getClassDate());
            existingClass.setZoomLink(updatedClass.getZoomLink());
            zoomClassRepository.save(existingClass);
            HashMap<String,Object> response = new HashMap<>();
            response.put("message", "Zoom class updated successfully");
            response.put("status", HttpStatus.OK.value());
            response.put("zoomClass", existingClass);
            return ResponseEntity.ok(response);
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating Zoom class: " + e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllClasses() {
        try{
            HashMap<String,Object> response = new HashMap<>();
            response.put("message", "Fetched all Zoom classes successfully");
            response.put("status", HttpStatus.OK.value());
            response.put("zoomClass", zoomClassRepository.findAll());
            return ResponseEntity.ok(response);
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching Zoom classes: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")// Assuming you have a mapping here// Ensures both deletes happen, or neither happens if one fails
    public ResponseEntity<?> deleteClass(@PathVariable Long id) {
        try {
            if (!zoomClassRepository.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Zoom class not found with id: " + id);
            }

            // 1. Delete all access records for this class first
            classAccessRepository.deleteByZoomClassId(id);

            // 2. Then delete the class itself
            zoomClassRepository.deleteById(id);

            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Zoom class and associated accesses deleted successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting Zoom class: " + e.getMessage());
        }
    }
}
