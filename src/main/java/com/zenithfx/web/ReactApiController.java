package com.zenithfx.web;

import com.zenithfx.model.Student;
import com.zenithfx.repository.StudentRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/react")
@CrossOrigin(origins = "*") // Allows React to talk to Spring Boot
public class ReactApiController {
    private final StudentRepository repository;

    public ReactApiController(StudentRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/students")
    public List<Student> getAllStudents() { return repository.findAll(); }

    @PostMapping("/students")
    public Student createStudent(@RequestBody Student student) { return repository.save(student); }

    @PutMapping("/students/{id}")
    public Student updateStudent(@PathVariable Long id, @RequestBody Student studentDetails) {
        Student student = repository.findById(id).orElseThrow();
        student.setName(studentDetails.getName());
        student.setNeptunCode(studentDetails.getNeptunCode());
        student.setCourse(studentDetails.getCourse());
        return repository.save(student);
    }

    @DeleteMapping("/students/{id}")
    public void deleteStudent(@PathVariable Long id) { repository.deleteById(id); }
}