package com.zenithfx.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    @GetMapping({"/", "/home"})
    public String home() { return "index"; }

    @GetMapping("/soap")
    public String soap() { return "soap"; }

    @GetMapping("/account")
    public String account() { return "account"; }

    @GetMapping("/prices")
    public String prices() { return "prices"; }

    @GetMapping("/history")
    public String history() { return "history"; }

    @GetMapping("/positions")
    public String positions() { return "positions"; }

    @GetMapping("/trade")
    public String trade() { return "trade"; }
}
