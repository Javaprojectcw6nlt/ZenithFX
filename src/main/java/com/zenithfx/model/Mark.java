package com.zenithfx.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;

@Entity
public class Mark {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long studentid;
    private LocalDate mdste;
    private Integer mark;
    private String type;
    private Long subjectid;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStudentid() { return studentid; }
    public void setStudentid(Long studentid) { this.studentid = studentid; }
    public LocalDate getMdste() { return mdste; }
    public void setMdste(LocalDate mdste) { this.mdste = mdste; }
    public Integer getMark() { return mark; }
    public void setMark(Integer mark) { this.mark = mark; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getSubjectid() { return subjectid; }
    public void setSubjectid(Long subjectid) { this.subjectid = subjectid; }
}