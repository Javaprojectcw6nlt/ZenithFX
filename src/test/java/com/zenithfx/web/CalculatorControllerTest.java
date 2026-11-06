package com.zenithfx.web;

import com.zenithfx.service.CalculatorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Web-layer tests: only the controller (and the shared ControllerAdvice) is loaded,
 * so no database or OANDA connection is needed.
 */
@WebMvcTest(CalculatorController.class)
@Import(CalculatorService.class)
class CalculatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions calculate(String a, String b, String operation) throws Exception {
        return mockMvc.perform(post("/calculator")
                .param("a", a)
                .param("b", b)
                .param("operation", operation));
    }

    @Test
    void getFormReturnsCalculatorView() throws Exception {
        mockMvc.perform(get("/calculator"))
                .andExpect(status().isOk())
                .andExpect(view().name("calculator"))
                .andExpect(model().attribute("active", "calculator"));
    }

    @Test
    void formContainsAllFourOperations() throws Exception {
        mockMvc.perform(get("/calculator"))
                .andExpect(content().string(containsString("value=\"add\"")))
                .andExpect(content().string(containsString("value=\"subtract\"")))
                .andExpect(content().string(containsString("value=\"multiply\"")))
                .andExpect(content().string(containsString("value=\"divide\"")));
    }

    @Test
    void additionShowsResult() throws Exception {
        calculate("2", "3", "add")
                .andExpect(status().isOk())
                .andExpect(view().name("calculator"))
                .andExpect(model().attribute("result", "5"))
                .andExpect(content().string(containsString("Result: 5")));
    }

    @Test
    void subtractionShowsResult() throws Exception {
        calculate("10", "4", "subtract")
                .andExpect(model().attribute("result", "6"));
    }

    @Test
    void multiplicationShowsResult() throws Exception {
        calculate("6", "7", "multiply")
                .andExpect(model().attribute("result", "42"));
    }

    @Test
    void divisionShowsDecimalResult() throws Exception {
        calculate("10", "4", "divide")
                .andExpect(model().attribute("result", "2.5"));
    }

    @Test
    void divisionByZeroShowsError() throws Exception {
        calculate("5", "0", "divide")
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "Division by zero is not allowed."))
                .andExpect(model().attributeDoesNotExist("result"))
                .andExpect(content().string(containsString("Division by zero is not allowed.")));
    }

    @Test
    void invalidNumberShowsError() throws Exception {
        calculate("abc", "3", "add")
                .andExpect(model().attribute("error", "Please enter valid numbers."))
                .andExpect(model().attributeDoesNotExist("result"));
    }

    @Test
    void unknownOperationShowsError() throws Exception {
        calculate("2", "3", "power")
                .andExpect(model().attribute("error", "Unknown operation: power"));
    }

    @Test
    void enteredValuesAreKeptInTheForm() throws Exception {
        calculate("2", "3", "multiply")
                .andExpect(model().attribute("a", "2"))
                .andExpect(model().attribute("b", "3"))
                .andExpect(model().attribute("operation", "multiply"));
    }

    @Test
    void missingParameterReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/calculator")
                        .param("b", "3")
                        .param("operation", "add"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void footerWithCreatorLineIsAvailable() throws Exception {
        mockMvc.perform(get("/calculator"))
                .andExpect(model().attributeExists("creatorLine"))
                .andExpect(content().string(containsString("Academic project")));
    }
}
