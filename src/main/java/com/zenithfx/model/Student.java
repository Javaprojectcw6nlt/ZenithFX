package com.zenithfx.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String sname; 
    private String studentClass; 
    private Integer boy; 

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSname() { return sname; }
    public void setSname(String sname) { this.sname = sname; }

    public String getStudentClass() { return studentClass; }
    public void setStudentClass(String studentClass) { this.studentClass = studentClass; }

    public Integer getBoy() { return boy; }
    public void setBoy(Integer boy) { this.boy = boy; }
}