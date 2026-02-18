package com.example.SIA.controller;

import com.example.SIA.exception.BadGatewayException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestErrorController {

    @GetMapping("/test502")
    public String trigger502() {
        throw new BadGatewayException("Simulación de error 502");
    }
}