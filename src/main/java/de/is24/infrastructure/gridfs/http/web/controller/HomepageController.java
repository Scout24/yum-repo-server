package de.is24.infrastructure.gridfs.http.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomepageController {

  @RequestMapping("/")
  public String home() {
    return "redirect:/repo/";
  }
}
