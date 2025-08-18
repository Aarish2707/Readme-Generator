package com.project.Readme.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class WebController {

    @RequestMapping(value = {"/dashboard", "/login"})
    public String forward() {
        return "forward:/index.html";
    }
}