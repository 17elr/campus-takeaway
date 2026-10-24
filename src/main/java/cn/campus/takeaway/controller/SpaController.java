package cn.campus.takeaway.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {
    @GetMapping({"/login","/register","/student","/student/{page:shops|orders|cart|addresses}",
        "/shops/{id:[0-9]+}","/orders/{id:[0-9]+}","/merchant","/merchant/{page:orders|dishes|shop}",
        "/rider","/rider/{page:available|orders}","/admin","/admin/{page:users|shops|dishes|orders}","/account"})
    String app() { return "forward:/index.html"; }
    @GetMapping("/home")
    String home() { return "redirect:/"; }
}
