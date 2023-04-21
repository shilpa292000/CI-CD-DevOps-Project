package com.jhooq.jhooqk8s.ws;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class jhooqDockerDemoController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello - Application is working fine";
    }
}
