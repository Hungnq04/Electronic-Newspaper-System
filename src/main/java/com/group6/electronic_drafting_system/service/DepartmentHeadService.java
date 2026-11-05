package com.group6.electronic_drafting_system.service;
import com.group6.electronic_drafting_system.dto.DepartmentHead.DashboardDTO;

public interface DepartmentHeadService {
    DashboardDTO getDashboard(String email);
}
