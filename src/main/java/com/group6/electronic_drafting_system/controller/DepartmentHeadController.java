package com.group6.electronic_drafting_system.controller;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/department-head")
public class DepartmentHeadController {

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Duyệt bản thảo – Trưởng ban");
        model.addAttribute("contentView", "department_head/dashboard");
        model.addAttribute("contentFragment", "departmentHeadContent");
        model.addAttribute("pageCss", List.of("/department_head/css/department-head-dashboard.css"));
        model.addAttribute("pageJs", List.of("/department_head/js/department-head-dashboard.js"));
        return "layout/app";
    }
}
