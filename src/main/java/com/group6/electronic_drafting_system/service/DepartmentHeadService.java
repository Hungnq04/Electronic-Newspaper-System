package com.group6.electronic_drafting_system.service;

import com.group6.electronic_drafting_system.dto.DepartmentHead.DepartmentHeadDashboardDTO;

public interface DepartmentHeadService {
    DepartmentHeadDashboardDTO getDashboard(String email);
}
