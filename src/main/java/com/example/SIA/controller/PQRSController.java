package com.example.SIA.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PQRSController {
    @GetMapping("/pqrs")
    public String pqrs() {
        return "error404";
    }
}