package com.zenithfx.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class PageModelAdvice {
    @Value("${app.creator-line:Creator Name - NEPTUN_CODE | Teammate Name - NEPTUN_CODE}")
    private String creatorLine;

    @ModelAttribute("creatorLine")
    public String creatorLine() {
        return creatorLine;
    }
}
