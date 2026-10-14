package com.group6.electronic_drafting_system.service;

import com.group6.electronic_drafting_system.dto.Reporter.ReporterTaskBoardDTO;
import com.group6.electronic_drafting_system.dto.Reporter.ReporterTaskDetailDTO;
import com.group6.electronic_drafting_system.service.MediaStorageService.MediaKind;
import org.springframework.web.multipart.MultipartFile;

public interface ReporterTaskService {
    ReporterTaskBoardDTO getBoard(String email);
    ReporterTaskDetailDTO getDetail(Integer assignmentId, String email);
    MediaKind uploadTopicMedia(Integer assignmentId, MultipartFile file, String caption, String email);
}
