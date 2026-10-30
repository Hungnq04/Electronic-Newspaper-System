package com.group6.electronic_drafting_system.controller;

import com.group6.electronic_drafting_system.dto.topic.EditorialTopicForm;
import com.group6.electronic_drafting_system.dto.topic.TopicAssignForm;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.service.EditorialTopicService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// FL-02: Editorial Topic List, Create Editorial Topic, Assign / Reassign Topic to Department Head
@Controller
@RequestMapping("/secretary/topics")
public class EditorialTopicController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EditorialTopicController.class);
    private static final String LIST = "redirect:/secretary/topics";

    private final EditorialTopicService editorialTopicService;

    public EditorialTopicController(EditorialTopicService editorialTopicService) {
        this.editorialTopicService = editorialTopicService;
    }

    // Hiển thị trong layout chung (sidebar + nội dung), xem templates/layout/app.html
    private static String page(Model model, String title, String view) {
        model.addAttribute("pageTitle", title + " | CMS Core");
        model.addAttribute("contentView", view);
        model.addAttribute("contentFragment", "content");
        model.addAttribute("pageCss", List.of("/css/secretary.css"));
        return "layout/app";
    }

    @GetMapping
    public String list(Principal principal, Model model) {
        model.addAttribute("unassigned", editorialTopicService.getUnassignedTopics(principal.getName()));
        model.addAttribute("assigned", editorialTopicService.getAssignedTopics(principal.getName()));
        return page(model, "Topic tòa soạn", "secretary/editorial-topic-list");
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("form")) model.addAttribute("form", new EditorialTopicForm());
        model.addAttribute("categories", editorialTopicService.getCategorySuggestions());
        return page(model, "Tạo Editorial Topic", "secretary/editorial-topic-create");
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") EditorialTopicForm form, BindingResult binding,
                         Principal principal, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("categories", editorialTopicService.getCategorySuggestions());
            return page(model, "Tạo Editorial Topic", "secretary/editorial-topic-create");
        }
        try {
            Integer topicId = editorialTopicService.createTopic(form, principal.getName());
            redirect.addFlashAttribute("message", "Đã tạo Topic. Chọn Trưởng phòng để giao Topic.");
            return "redirect:/secretary/topics/" + topicId + "/assign";
        } catch (ApplicationException ex) {
            model.addAttribute("error", ex.getErrorCode().getMessage());
        } catch (Exception ex) {
            log.error("Create editorial topic failed", ex);
            model.addAttribute("error", ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());
        }
        model.addAttribute("categories", editorialTopicService.getCategorySuggestions());
        return page(model, "Tạo Editorial Topic", "secretary/editorial-topic-create");
    }

    @GetMapping("/{topicId}/assign")
    public String assignForm(@PathVariable Integer topicId, Principal principal, Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("page", editorialTopicService.getAssignPage(topicId, principal.getName()));
            if (!model.containsAttribute("form")) model.addAttribute("form", new TopicAssignForm());
            return page(model, "Giao Topic cho Trưởng phòng", "secretary/topic-assign");
        } catch (ApplicationException ex) {
            redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
            return LIST;
        }
    }

    @PostMapping("/{topicId}/assign")
    public String assign(@PathVariable Integer topicId, @Valid @ModelAttribute("form") TopicAssignForm form,
                         BindingResult binding, Principal principal, RedirectAttributes redirect) {
        String back = "redirect:/secretary/topics/" + topicId + "/assign";
        if (binding.hasErrors()) {
            redirect.addFlashAttribute("error", binding.getAllErrors().get(0).getDefaultMessage());
            redirect.addFlashAttribute("form", form);
            return back;
        }
        try {
            editorialTopicService.assignDepartmentHead(topicId, form, principal.getName());
            redirect.addFlashAttribute("message", "Đã giao Topic cho Trưởng phòng.");
            return LIST;
        } catch (ApplicationException ex) {
            redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
        } catch (Exception ex) {
            log.error("Assign topic {} failed", topicId, ex);
            redirect.addFlashAttribute("error", ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());
        }
        redirect.addFlashAttribute("form", form);
        return back;
    }
}
