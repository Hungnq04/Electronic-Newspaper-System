package com.group6.electronic_drafting_system.service;

import com.group6.electronic_drafting_system.dto.Reporter.ReporterTaskBoardDTO;
import com.group6.electronic_drafting_system.dto.Reporter.ReporterTaskDetailDTO;

public interface ReporterTaskService {
    ReporterTaskBoardDTO getBoard(String email);
    ReporterTaskDetailDTO getDetail(Integer assignmentId, String email);
}
