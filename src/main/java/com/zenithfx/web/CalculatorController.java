package com.zenithfx.web;

import com.zenithfx.service.CalculatorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
public class CalculatorController {

    private final CalculatorService calculator;

    public CalculatorController(CalculatorService calculator) {
        this.calculator = calculator;
    }

    @GetMapping("/calculator")
    public String showForm(Model model) {
        model.addAttribute("active", "calculator");
        model.addAttribute("operation", "add");
        return "calculator";
    }

    @PostMapping("/calculator")
    public String calculate(@RequestParam String a,
                            @RequestParam String b,
                            @RequestParam String operation,
                            Model model) {
        model.addAttribute("active", "calculator");
        model.addAttribute("a", a);
        model.addAttribute("b", b);
        model.addAttribute("operation", operation);
        try {
            BigDecimal result = calculator.calculate(new BigDecimal(a.trim()), new BigDecimal(b.trim()), operation);
            model.addAttribute("result", result.toPlainString());
        } catch (NumberFormatException e) {
            model.addAttribute("error", "Please enter valid numbers.");
        } catch (ArithmeticException | IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "calculator";
    }
}
