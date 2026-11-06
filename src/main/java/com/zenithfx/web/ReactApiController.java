package com.zenithfx.web;

import com.zenithfx.model.Mark;
import com.zenithfx.model.Student;
import com.zenithfx.model.Subject;
import com.zenithfx.repository.MarkRepository;
import com.zenithfx.repository.StudentRepository;
import com.zenithfx.repository.SubjectRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/react")
@CrossOrigin(origins = "*")
public class ReactApiController {
    private final StudentRepository studentRepo;
    private final SubjectRepository subjectRepo;
    private final MarkRepository markRepo;

    public ReactApiController(StudentRepository studentRepo, SubjectRepository subjectRepo, MarkRepository markRepo) {
        this.studentRepo = studentRepo;
        this.subjectRepo = subjectRepo;
        this.markRepo = markRepo;
    }

    // ---------- STUDENTS ----------
    @GetMapping("/students")
    public List<Student> getAllStudents() { return studentRepo.findAll(); }

    @PostMapping("/students")
    public Student createStudent(@RequestBody Student student) { return studentRepo.save(student); }

    @PutMapping("/students/{id}")
    public Student updateStudent(@PathVariable Long id, @RequestBody Student details) {
        Student s = studentRepo.findById(id).orElseThrow();
        s.setSname(details.getSname());
        s.setStudentClass(details.getStudentClass());
        s.setBoy(details.getBoy());
        return studentRepo.save(s);
    }

    @DeleteMapping("/students/{id}")
    public void deleteStudent(@PathVariable Long id) { studentRepo.deleteById(id); }

    // ---------- SUBJECTS ----------
    @GetMapping("/subjects")
    public List<Subject> getAllSubjects() { return subjectRepo.findAll(); }

    @PostMapping("/subjects")
    public Subject createSubject(@RequestBody Subject subject) { return subjectRepo.save(subject); }

    @PutMapping("/subjects/{id}")
    public Subject updateSubject(@PathVariable Long id, @RequestBody Subject details) {
        Subject s = subjectRepo.findById(id).orElseThrow();
        s.setName(details.getName());
        s.setCategory(details.getCategory());
        return subjectRepo.save(s);
    }

    @DeleteMapping("/subjects/{id}")
    public void deleteSubject(@PathVariable Long id) { subjectRepo.deleteById(id); }

    // ---------- MARKS ----------
    @GetMapping("/marks")
    public List<Mark> getAllMarks() { return markRepo.findAll(); }

    @PostMapping("/marks")
    public Mark createMark(@RequestBody Mark mark) { return markRepo.save(mark); }

    @PutMapping("/marks/{id}")
    public Mark updateMark(@PathVariable Long id, @RequestBody Mark details) {
        Mark m = markRepo.findById(id).orElseThrow();
        m.setStudentid(details.getStudentid());
        m.setMdste(details.getMdste());
        m.setMark(details.getMark());
        m.setType(details.getType());
        m.setSubjectid(details.getSubjectid());
        return markRepo.save(m);
    }

    @DeleteMapping("/marks/{id}")
    public void deleteMark(@PathVariable Long id) { markRepo.deleteById(id); }
}