package com.group6.electronic_drafting_system.service.impl;

import com.group6.electronic_drafting_system.dto.Reporter.ReporterTaskBoardDTO;
import com.group6.electronic_drafting_system.dto.Reporter.ReporterTaskDTO;
import com.group6.electronic_drafting_system.dto.Reporter.ReporterTaskDetailDTO;
import com.group6.electronic_drafting_system.dto.Reporter.TaskStage;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Article;
import com.group6.electronic_drafting_system.entity.Assignment;
import com.group6.electronic_drafting_system.entity.Source;
import com.group6.electronic_drafting_system.entity.Topic;
import com.group6.electronic_drafting_system.entity.TopicSource;
import com.group6.electronic_drafting_system.entity.enums.AssignmentRole;
import com.group6.electronic_drafting_system.entity.enums.AssignmentStatus;
import com.group6.electronic_drafting_system.entity.enums.Priority;
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
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
    // Nhiệm vụ còn hạn dưới mốc này được xếp vào "Cần nộp gấp"
    private static final Duration URGENT_WINDOW = Duration.ofHours(24);
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

    // Spring tự truyền các repository / service cần dùng vào đây (constructor injection)
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

    // Trang "Nhiệm vụ tác nghiệp": mọi nhiệm vụ LEAD / SUPPORTING còn hiệu lực của phóng viên
    // trên đề tài ACTIVE, kèm bài viết, số tư liệu, thống kê và các cột Kanban.
    @Override
    @Transactional(readOnly = true)
    public ReporterTaskBoardDTO getBoard(String email) {
        Account me = currentAccount(email);
        OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        // Lấy tất cả nhiệm vụ của chính mình (viết chính / hỗ trợ)
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
            return toTaskDTO(a, article, sourceTypes, now);
        }).sorted(BY_DEADLINE).toList();
        // Đếm số nhiệm vụ theo nhóm (todo / doing / review / done) và gói thành dữ liệu trang
        return board(me, tasks);
    }

    // Trang chi tiết một nhiệm vụ của chính mình: đề tài, nhóm thực hiện, tư liệu (đề tài + bài viết) và bài viết.
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
                .forEach(ts -> byId.put(ts.getSource().getId(), toSourceItem(ts.getSource(), ts.getNote(), "Đề tài")));
        if (article != null) articleSources.findByArticleOrdered(article.getId()).forEach(as -> {
            String note = as.getCaption() != null ? as.getCaption() : as.getNote();
            byId.merge(as.getSource().getId(), toSourceItem(as.getSource(), note, "Bài viết"),
                    (onTopic, onArticle) -> toSourceItem(as.getSource(), note != null ? note : onTopic.note(), "Đề tài · Bài viết"));
        });
        List<ReporterTaskDetailDTO.SourceItem> sources = List.copyOf(byId.values());

        ReporterTaskDTO task = toTaskDTO(assignment, article,
                sources.stream().map(ReporterTaskDetailDTO.SourceItem::type).toList(),
                OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        // Trưởng ban -> viết chính -> hỗ trợ (theo thứ tự khai báo của AssignmentRole)
        List<ReporterTaskDetailDTO.Member> team = assignments.findByTopicAndStatusNot(topic, AssignmentStatus.REMOVED).stream()
                .sorted(Comparator.comparing(Assignment::getAssignmentRole))
                .map(a -> mapper.toMember(a, initials(a.getAccount().getFullName()), roleLabel(a.getAssignmentRole()),
                        a.getAccount().getId().equals(me.getId())))
                .toList();

        return new ReporterTaskDetailDTO(task, me.getFullName(), departmentName(me), initials(me.getFullName()),
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
            // Lưu DB lỗi thì xóa tệp vừa tải lên Cloudinary, tránh để lại tệp "mồ côi"
            mediaStorage.delete(stored);
            throw ex;
        }
    }

    // ===================== Tính dữ liệu cho một nhiệm vụ =====================

    // Tính mọi giá trị hiển thị của một nhiệm vụ (tiến độ, quá hạn, gấp, đếm tư liệu, link viết bài...)
    // rồi đưa cho mapper đóng gói thành ReporterTaskDTO.
    // sourceTypes: loại của các tư liệu gắn với đề tài / bài viết, dùng để đếm ảnh, tài liệu
    private ReporterTaskDTO toTaskDTO(Assignment assignment, Article article, Collection<SourceType> sourceTypes, OffsetDateTime now) {
        Topic topic = assignment.getTopic();
        TaskStage stage = stageOf(assignment, article);
        OffsetDateTime deadline = assignment.getDeadline();
        boolean overdue = stage.isOpen() && deadline != null && deadline.isBefore(now);
        boolean urgent = stage.isOpen() && (topic.getPriority() == Priority.URGENT
                || (deadline != null && deadline.isBefore(now.plus(URGENT_WINDOW))));

        // Đếm tư liệu theo loại
        int images = (int) sourceTypes.stream().filter(t -> t == SourceType.IMAGE).count();
        int documents = (int) sourceTypes.stream().filter(t -> t == SourceType.DOCUMENT || t == SourceType.INTERNAL_DOC).count();
        int others = sourceTypes.size() - images - documents;

        // Chỉ người viết chính (LEAD) mới mở trình soạn thảo được
        boolean lead = assignment.getAssignmentRole() == AssignmentRole.LEAD;
        // Một đề tài có nhiều bài (BR-01): nhiệm vụ viết chính còn hiệu lực trên đề tài ACTIVE luôn viết thêm bài được
        boolean canCreateArticle = lead && assignment.getStatus() == AssignmentStatus.ASSIGNED
                && topic.getStatus() == TopicStatus.ACTIVE;
        String newArticleHref = canCreateArticle ? "/articles/new?assignmentId=" + assignment.getId() : null;
        String writeHref = null;
        if (lead) writeHref = article == null ? newArticleHref : "/articles/" + article.getId() + "/edit";

        return mapper.toTaskDTO(assignment, article, priorityLabel(topic.getPriority()), roleLabel(assignment.getAssignmentRole()), lead,
                initials(assignment.getAssignedBy().getFullName()),
                deadlineText(stage, deadline, article == null ? null : article.getSubmittedAt(), now), deadlineBucket(deadline, now),
                images, documents, others, sourceTypes.size(),
                stage, urgent, overdue, canCreateArticle,
                writeHref, newArticleHref, writeLabel(stage));
    }

    // Tiến độ nhiệm vụ: assignment COMPLETED là hoàn thành hẳn; còn lại suy ra từ trạng thái bài viết
    // (đã nộp / đã duyệt thì coi như xong phần viết)
    private static TaskStage stageOf(Assignment assignment, Article article) {
        if (assignment.getStatus() == AssignmentStatus.COMPLETED) return TaskStage.DONE;
        if (article == null) return TaskStage.NOT_STARTED;
        return switch (article.getStatus()) {
            case DRAFT -> TaskStage.IN_PROGRESS;
            case REVISION_REQUIRED -> TaskStage.REVISION;
            case DEPARTMENT_REVIEW, DEPARTMENT_APPROVED -> TaskStage.IN_REVIEW;
            case APPROVED, SCHEDULED, PUBLISHED -> TaskStage.DONE;
        };
    }

    // Chữ trên nút viết bài, theo tiến độ
    private static String writeLabel(TaskStage stage) {
        return switch (stage) {
            case NOT_STARTED -> "Bắt đầu viết";
            case IN_PROGRESS -> "Tiếp tục viết";
            case REVISION -> "Sửa bài";
            case IN_REVIEW, DONE -> "Xem bài";
        };
    }

    // Việc còn mở: "Còn 2 ngày 3 giờ" / "Quá hạn 5 giờ 10 phút".
    // Việc đã nộp: so lúc nộp với deadline -> "Nộp đúng hạn" / "Nộp trễ hạn". Không có deadline thì null.
    private static String deadlineText(TaskStage stage, OffsetDateTime deadline, OffsetDateTime submittedAt, OffsetDateTime now) {
        if (deadline == null) return null;
        if (!stage.isOpen()) {
            if (submittedAt == null) return null;
            return submittedAt.isAfter(deadline) ? "Nộp trễ hạn" : "Nộp đúng hạn";
        }
        Duration left = Duration.between(now, deadline);
        return (left.isNegative() ? "Quá hạn " : "Còn ") + formatDuration(left.abs());
    }

    // Đổi khoảng thời gian thành chữ: "2 ngày 3 giờ", "5 giờ 10 phút", "45 phút" (ít nhất 1 phút)
    private static String formatDuration(Duration d) {
        if (d.toDays() > 0) return d.toDays() + " ngày" + (d.toHoursPart() > 0 ? " " + d.toHoursPart() + " giờ" : "");
        if (d.toHours() > 0) return d.toHours() + " giờ " + d.toMinutesPart() + " phút";
        return Math.max(d.toMinutes(), 1) + " phút";
    }

    // Nhóm deadline cho bộ lọc theo hạn: overdue (quá hạn) / today (hôm nay) / week (7 ngày tới) / later / none (không có hạn)
    private static String deadlineBucket(OffsetDateTime deadline, OffsetDateTime now) {
        if (deadline == null) return "none";
        if (deadline.isBefore(now)) return "overdue";
        LocalDate day = deadline.atZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
        LocalDate today = now.atZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
        if (day.equals(today)) return "today";
        return day.isBefore(today.plusDays(7)) ? "week" : "later";
    }

    // Tính nhãn loại + cờ ảnh / video của một tư liệu rồi đưa cho mapper đóng gói.
    // Video được lưu với source_type khác IMAGE, nhận ra qua đường dẫn Cloudinary.
    private ReporterTaskDetailDTO.SourceItem toSourceItem(Source source, String note, String origin) {
        boolean video = source.getSourceType() != SourceType.IMAGE && MediaStorageService.isVideoUrl(source.getFileUrl());
        boolean image = source.getSourceType() == SourceType.IMAGE && source.getFileUrl() != null && !source.getFileUrl().isBlank();
        String typeLabel = video ? "Video" : sourceTypeLabel(source.getSourceType());
        String addedByName = source.getAddedBy() == null ? null : source.getAddedBy().getFullName();
        return mapper.toSourceItem(source, typeLabel, note, addedByName, origin, image, video);
    }

    // ===================== Dữ liệu chung của trang =====================

    // Gói danh sách nhiệm vụ thành dữ liệu trang: đếm theo nhóm, chia cột Kanban, danh sách chuyên mục để lọc
    private ReporterTaskBoardDTO board(Account me, List<ReporterTaskDTO> tasks) {
        long todo = countGroup(tasks, "todo");
        long doing = countGroup(tasks, "doing");
        long review = countGroup(tasks, "review");
        long done = countGroup(tasks, "done");
        List<ReporterTaskBoardDTO.Column> columns = List.of(
                column("todo", "Chưa bắt đầu", tasks),
                column("doing", "Đang thực hiện", tasks),
                column("review", "Chờ duyệt", tasks),
                column("done", "Hoàn thành", tasks));
        List<String> categories = tasks.stream().map(ReporterTaskDTO::category).distinct().sorted().toList();
        long urgentCount = tasks.stream().filter(ReporterTaskDTO::urgent).count();
        return new ReporterTaskBoardDTO(me.getFullName(), departmentName(me), initials(me.getFullName()),
                tasks, urgentCount, todo, doing, review, done, todo + doing, categories, columns);
    }

    // Đếm số nhiệm vụ thuộc một nhóm (todo / doing / review / done)
    private static long countGroup(List<ReporterTaskDTO> tasks, String group) {
        return tasks.stream().filter(t -> t.stage().getGroup().equals(group)).count();
    }

    // Một cột Kanban: các nhiệm vụ thuộc nhóm key
    private static ReporterTaskBoardDTO.Column column(String key, String label, List<ReporterTaskDTO> tasks) {
        return new ReporterTaskBoardDTO.Column(key, label, tasks.stream().filter(t -> t.stage().getGroup().equals(key)).toList());
    }

    // Một đề tài có thể có nhiều bài (BR-01): lấy bài mới cập nhật nhất của mình, người hỗ trợ xem bài mới nhất của đề tài
    private static Article pickArticle(List<Article> topicArticles, Integer myId) {
        Comparator<Article> byUpdated = Comparator.comparing(Article::getUpdatedAt);
        return topicArticles.stream().filter(a -> a.getLead().getId().equals(myId)).max(byUpdated)
                .orElseGet(() -> topicArticles.stream().max(byUpdated).orElseThrow());
    }

    // ===================== Kiểm tra quyền / tìm dữ liệu =====================

    // Kiểm tra quyền: nhiệm vụ phải của chính mình (viết chính / hỗ trợ), chưa bị gỡ, trên đề tài đang triển khai
    private Assignment myTask(Integer assignmentId, Account me) {
        return assignments.findWithDetailsById(assignmentId)
                .filter(a -> a.getAccount().getId().equals(me.getId()))
                .filter(a -> a.getStatus() != AssignmentStatus.REMOVED && REPORTER_ROLES.contains(a.getAssignmentRole()))
                .filter(a -> a.getTopic().getStatus() == TopicStatus.ACTIVE)
                .orElseThrow(() -> new ApplicationException(ErrorCode.TASK_NOT_FOUND));
    }

    // Tài khoản đang đăng nhập (tìm theo email); không thấy thì báo lỗi
    private Account currentAccount(String email) {
        if (email == null) throw new ApplicationException(ErrorCode.TASK_NOT_FOUND);
        return accounts.findByEmailIgnoreCase(email.trim()).orElseThrow(() -> new ApplicationException(ErrorCode.TASK_NOT_FOUND));
    }

    // ===================== Hàm phụ: chuỗi và nhãn hiển thị =====================

    // Tên tư liệu khi người dùng không nhập chú thích: lấy tên tệp (tối đa 255 ký tự)
    private static String fileTitle(String filename) {
        String name = filename == null || filename.isBlank() ? "Tệp tải lên" : filename.trim();
        return trimTo(name, 255);
    }

    // Bỏ khoảng trắng hai đầu và cắt còn tối đa max ký tự; chuỗi rỗng thì trả null
    private static String trimTo(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    // Tên phòng ban của tài khoản (null nếu chưa gán phòng)
    private static String departmentName(Account account) {
        return account.getDepartment() == null ? null : account.getDepartment().getDepartmentName();
    }

    // Chữ hiển thị của mức ưu tiên đề tài
    private static String priorityLabel(Priority priority) {
        return switch (priority) {
            case URGENT -> "Hỏa tốc";
            case HIGH -> "Ưu tiên cao";
            case NORMAL -> "Thường";
            case LOW -> "Thấp";
        };
    }

    // Chữ hiển thị của vai trò trong đề tài
    private static String roleLabel(AssignmentRole role) {
        return switch (role) {
            case LEAD -> "Viết chính";
            case SUPPORTING -> "Hỗ trợ tác nghiệp";
            case DEPARTMENT_HEAD -> "Trưởng ban phụ trách";
        };
    }

    // Chữ hiển thị của loại tư liệu
    private static String sourceTypeLabel(SourceType type) {
        return switch (type) {
            case IMAGE -> "Ảnh";
            case DOCUMENT -> "Tài liệu";
            case WEBSITE -> "Website";
            case INTERVIEW -> "Phỏng vấn";
            case INTERNAL_DOC -> "Nội bộ";
        };
    }

    // Chữ viết tắt cho avatar: "Nguyễn Thu Hà" -> "TH" (chữ cái đầu của hai từ cuối)
    private static String initials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] words = fullName.trim().split("\\s+");
        return Arrays.stream(words, Math.max(0, words.length - 2), words.length)
                .map(w -> w.substring(0, 1))
                .collect(Collectors.joining())
                .toUpperCase(Locale.ROOT);
    }
}
