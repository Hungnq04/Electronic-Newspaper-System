package com.group6.electronic_drafting_system.dto.DepartmentHead;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

//Quy định Dashboard lấy ra những thông tin gì từ database ra
@Getter 
@Builder 
public class DashboardDTO {
    private String email;
    private String username;
    private String fullName;
    private long pendingArticlesCount;
    private List<PendingArticleDTO> pendingArticles;
    private long rC;
    private long chapthuanngayhomnay;
    private long detaidangchoduyet;
}
