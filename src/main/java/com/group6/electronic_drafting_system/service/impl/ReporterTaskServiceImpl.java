package com.group6.electronic_drafting_system.service.impl;

import com.group6.electronic_drafting_system.dto.Reporter.ReporterTaskBoardDTO;
import com.group6.electronic_drafting_system.dto.Reporter.ReporterTaskDTO;
import com.group6.electronic_drafting_system.dto.Reporter.ReporterTaskDetailDTO;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Article;
import com.group6.electronic_drafting_system.entity.Assignment;
import com.group6.electronic_drafting_system.entity.Source;
import com.group6.electronic_drafting_system.entity.Topic;
import com.group6.electronic_drafting_system.entity.TopicSource;
import com.group6.electronic_drafting_system.entity.enums.AssignmentRole;
import com.group6.electronic_drafting_system.entity.enums.AssignmentStatus;
import com.group6.electronic_drafting_system.entity.enums.SourceType;
import com.group6.electronic_drafting_system.entity.enums.TopicStatus;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.mapper.Reporter.ReporterTaskMapper;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import com.group6.electronic_drafting_system.repository.ArticleRepository;
import com.group6.electronic_drafting_system.repository.ArticleSourceRepository;
import com.group6.electronic_drafting_system.repository.AssignmentRepository;
import com.group6.electronic_drafting_system.repository.SourceRepository;
import com.group6.electronic_drafting_system.repository.TopicSourceRepository;
import com.group6.electronic_drafting_system.service.MediaStorageService;
import com.group6.electronic_drafting_system.service.MediaStorageService.MediaKind;
import com.group6.electronic_drafting_system.service.MediaStorageService.StoredMedia;
import com.group6.electronic_drafting_system.service.ReporterTaskService;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ReporterTaskServiceImpl implements ReporterTaskService {
    private static final Set<AssignmentRole> REPORTER_ROLES = EnumSet.of(AssignmentRole.LEAD, AssignmentRole.SUPPORTING);
    // Việc còn mở lên trước (hạn gần nhất trước, không có hạn xuống cuối), việc đã xong xuống dưới
    private static final Comparator<ReporterTaskDTO> BY_DEADLINE = Comparator
            .comparing((ReporterTaskDTO t) -> !t.stage().isOpen())
            .thenComparing(ReporterTaskDTO::deadline, Comparator.nullsLast(Comparator.naturalOrder()));

    private final AssignmentRepository assignments;
    private final ArticleRepository articles;
    private final ArticleSourceRepository articleSources;
    private final TopicSourceRepository topicSources;
    private final AccountRepository accounts;
    private final SourceRepository sourceRepository;
    private final MediaStorageService mediaStorage;
    private final ReporterTaskMapper mapper;

    public ReporterTaskServiceImpl(AssignmentRepository assignments, ArticleRepository articles, ArticleSourceRepository articleSources,
                                   TopicSourceRepository topicSources, AccountRepository accounts, SourceRepository sourceRepository,
                                   MediaStorageService mediaStorage, ReporterTaskMapper mapper) {
        this.assignments = assignments;
        this.articles = articles;
        this.articleSources = articleSources;
        this.topicSources = topicSources;
        this.accounts = accounts;
        this.sourceRepository = sourceRepository;
        this.mediaStorage = mediaStorage;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public ReporterTaskBoardDTO getBoard(String email) {
        Account me = currentAccount(email);
        OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        List<Assignment> mine = assignments.findByAccount_IdAndAssignmentRoleInAndStatusNotAndTopic_Status(
                me.getId(), REPORTER_ROLES, AssignmentStatus.REMOVED, TopicStatus.ACTIVE);

        // Nạp một lần bài viết + tư liệu của tất cả đề tài, tránh truy vấn theo từng dòng
        List<Integer> topicIds = mine.stream().map(a -> a.getTopic().getId()).distinct().toList();
        if (topicIds.isEmpty()) return board(me, List.of());
        Map<Integer, List<Article>> articlesByTopic = articles.findByTopic_IdIn(topicIds).stream()
                .collect(Collectors.groupingBy(a -> a.getTopic().getId()));
        Map<Integer, Article> articleByTopic = articlesByTopic.keySet().stream()
                .collect(Collectors.toMap(Function.identity(), id -> pickArticle(articlesByTopic.get(id), me.getId())));
        Map<Integer, List<Source>> sourcesByTopic = topicSources.findByTopic_IdIn(topicIds).stream()
                .collect(Collectors.groupingBy(ts -> ts.getTopic().getId(), Collectors.mapping(ts -> ts.getSource(), Collectors.toList())));
        List<Integer> articleIds = articleByTopic.values().stream().map(Article::getId).toList();
        Map<Integer, List<Source>> sourcesByArticle = articleIds.isEmpty() ? Map.of()
                : articleSources.findByArticle_IdIn(articleIds).stream()
                .collect(Collectors.groupingBy(as -> as.getArticle().getId(), Collectors.mapping(as -> as.getSource(), Collectors.toList())));

        List<ReporterTaskDTO> tasks = mine.stream().map(a -> {
            Integer topicId = a.getTopic().getId();
            Article article = articleByTopic.get(topicId);
            List<SourceType> sourceTypes = Stream.concat(
                            sourcesByTopic.getOrDefault(topicId, List.of()).stream(),
                            article == null ? Stream.<Source>empty() : sourcesByArticle.getOrDefault(article.getId(), List.of()).stream())
                    .distinct().map(Source::getSourceType).toList();
            return mapper.toTaskDTO(a, article, sourceTypes, now);
        }).sorted(BY_DEADLINE).toList();
        return board(me, tasks);
    }

    @Override
    @Transactional(readOnly = true)
    public ReporterTaskDetailDTO getDetail(Integer assignmentId, String email) {
        Account me = currentAccount(email);
        Assignment assignment = myTask(assignmentId, me);
        Topic topic = assignment.getTopic();

        List<Article> topicArticles = articles.findByTopic_IdIn(List.of(topic.getId()));
        Article article = topicArticles.isEmpty() ? null : pickArticle(topicArticles, me.getId());

        // Một tư liệu có thể gắn cả vào đề tài lẫn bài viết: chỉ hiện một lần
        Map<Integer, ReporterTaskDetailDTO.SourceItem> byId = new LinkedHashMap<>();
        topicSources.findByTopic_IdOrderByAttachedAtAsc(topic.getId())
                .forEach(ts -> byId.put(ts.getSource().getId(), mapper.toSourceItem(ts.getSource(), ts.getNote(), "Đề tài")));
        if (article != null) articleSources.findByArticleOrdered(article.getId()).forEach(as -> {
            String note = as.getCaption() != null ? as.getCaption() : as.getNote();
            byId.merge(as.getSource().getId(), mapper.toSourceItem(as.getSource(), note, "Bài viết"),
                    (onTopic, onArticle) -> mapper.toSourceItem(as.getSource(), note != null ? note : onTopic.note(), "Đề tài · Bài viết"));
        });
        List<ReporterTaskDetailDTO.SourceItem> sources = List.copyOf(byId.values());

        ReporterTaskDTO task = mapper.toTaskDTO(assignment, article,
                sources.stream().map(ReporterTaskDetailDTO.SourceItem::type).toList(),
                OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        // Trưởng ban -> viết chính -> hỗ trợ (theo thứ tự khai báo của AssignmentRole)
        List<ReporterTaskDetailDTO.Member> team = assignments.findByTopicAndStatusNot(topic, AssignmentStatus.REMOVED).stream()
                .sorted(Comparator.comparing(Assignment::getAssignmentRole))
                .map(a -> mapper.toMember(a, me.getId()))
                .toList();

        return new ReporterTaskDetailDTO(task, me.getFullName(), departmentName(me), ReporterTaskMapper.initials(me.getFullName()),
                topic.getDescription(), topic.getRationale(), topic.getExpectedDate(), topic.getCreator().getFullName(),
                article == null ? null : article.getSapo(), article == null ? null : article.getSubmittedAt(),
                team, sources, assignment.getStatus() == AssignmentStatus.ASSIGNED);
    }

    // Phóng viên (hỗ trợ hoặc viết chính) đang làm nhiệm vụ tải ảnh / video lên Cloudinary và gắn vào tư liệu của đề tài.
    // Ảnh lưu source_type IMAGE; video lưu DOCUMENT (DB chưa có VIDEO; INTERVIEW là tư liệu nội bộ nên không dùng), nhận ra qua đường dẫn Cloudinary.
    @Override
    @Transactional
    public MediaKind uploadTopicMedia(Integer assignmentId, MultipartFile file, String caption, String email) {
        Account me = currentAccount(email);
        Assignment assignment = myTask(assignmentId, me);
        if (assignment.getStatus() != AssignmentStatus.ASSIGNED) throw new ApplicationException(ErrorCode.TASK_NOT_OPEN);
        Topic topic = assignment.getTopic();
        String note = trimTo(caption, 4000);
        StoredMedia stored = mediaStorage.upload(file, "topic-" + topic.getId());
        try {
            Source source = new Source();
            source.setAddedBy(me);
            source.setTitle(note != null ? trimTo(note, 255) : fileTitle(file.getOriginalFilename()));
            source.setSourceType(stored.kind() == MediaKind.VIDEO ? SourceType.DOCUMENT : SourceType.IMAGE);
            source.setFileUrl(stored.url());
            source.setDescription(stored.kind() == MediaKind.VIDEO ? "Video" : null);
            sourceRepository.save(source);

            TopicSource attached = new TopicSource();
            attached.setTopic(topic);
            attached.setSource(source);
            attached.setNote(note);
            topicSources.save(attached);
            return stored.kind();
        } catch (RuntimeException ex) {
            mediaStorage.delete(stored);
            throw ex;
        }
    }

    // Nhiệm vụ của chính mình (viết chính / hỗ trợ), chưa bị gỡ, trên đề tài đang triển khai
    private Assignment myTask(Integer assignmentId, Account me) {
        return assignments.findWithDetailsById(assignmentId)
                .filter(a -> a.getAccount().getId().equals(me.getId()))
                .filter(a -> a.getStatus() != AssignmentStatus.REMOVED && REPORTER_ROLES.contains(a.getAssignmentRole()))
                .filter(a -> a.getTopic().getStatus() == TopicStatus.ACTIVE)
                .orElseThrow(() -> new ApplicationException(ErrorCode.TASK_NOT_FOUND));
    }

    private static String fileTitle(String filename) {
        String name = filename == null || filename.isBlank() ? "Tệp tải lên" : filename.trim();
        return trimTo(name, 255);
    }

    private static String trimTo(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private static ReporterTaskBoardDTO board(Account me, List<ReporterTaskDTO> tasks) {
        Map<String, Long> groupCounts = tasks.stream().collect(Collectors.groupingBy(t -> t.stage().getGroup(), Collectors.counting()));
        List<ReporterTaskBoardDTO.Column> columns = List.of(
                column("todo", "Chưa bắt đầu", tasks),
                column("doing", "Đang thực hiện", tasks),
                column("review", "Chờ duyệt", tasks),
                column("done", "Hoàn thành", tasks));
        List<String> categories = tasks.stream().map(ReporterTaskDTO::category).distinct().sorted().toList();
        return new ReporterTaskBoardDTO(me.getFullName(), departmentName(me), ReporterTaskMapper.initials(me.getFullName()),
                tasks, tasks.stream().filter(ReporterTaskDTO::urgent).count(), groupCounts, categories, columns);
    }

    private static ReporterTaskBoardDTO.Column column(String key, String label, List<ReporterTaskDTO> tasks) {
        return new ReporterTaskBoardDTO.Column(key, label, tasks.stream().filter(t -> t.stage().getGroup().equals(key)).toList());
    }

    // Một đề tài có thể có nhiều bài (BR-01): lấy bài mới cập nhật nhất của mình, người hỗ trợ xem bài mới nhất của đề tài
    private static Article pickArticle(List<Article> topicArticles, Integer myId) {
        Comparator<Article> byUpdated = Comparator.comparing(Article::getUpdatedAt);
        return topicArticles.stream().filter(a -> a.getLead().getId().equals(myId)).max(byUpdated)
                .orElseGet(() -> topicArticles.stream().max(byUpdated).orElseThrow());
    }

    private static String departmentName(Account account) {
        return account.getDepartment() == null ? null : account.getDepartment().getDepartmentName();
    }

    private Account currentAccount(String email) {
        if (email == null) throw new ApplicationException(ErrorCode.TASK_NOT_FOUND);
        return accounts.findByEmailIgnoreCase(email.trim()).orElseThrow(() -> new ApplicationException(ErrorCode.TASK_NOT_FOUND));
    }
}
