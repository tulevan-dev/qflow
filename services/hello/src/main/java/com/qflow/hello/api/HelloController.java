package com.qflow.hello.api;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of(
                "service", "hello",
                "message", "QFlow skeleton is running");
    }
}
