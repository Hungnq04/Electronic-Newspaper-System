package com.group6.electronic_drafting_system.mapper.DepartmentHead;

import com.group6.electronic_drafting_system.dto.DepartmentHead.ContentBlockDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.ContentBlockDTO.Kind;
import com.group6.electronic_drafting_system.dto.DepartmentHead.ReviewCommentDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.SourceItemDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.VersionViewDTO;
import com.group6.electronic_drafting_system.entity.ArticleSource;
import com.group6.electronic_drafting_system.entity.ArticleVersion;
import com.group6.electronic_drafting_system.entity.Comment;
import com.group6.electronic_drafting_system.entity.Source;
import com.group6.electronic_drafting_system.entity.enums.AnchorField;
import com.group6.electronic_drafting_system.entity.enums.CommentStatus;
import com.group6.electronic_drafting_system.entity.enums.SourceType;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class ReviewWorkspaceMapper {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM");

    // Các thẻ được coi là MỘT đoạn chữ (một lần đánh số "Đoạn n")
    private static final Set<String> TEXT_TAGS = Set.of("p", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "pre", "li");
    // Các thẻ không phải chữ nằm trong dòng: gặp chúng thì kết thúc đoạn chữ đang gom
    private static final Set<String> BLOCK_TAGS = Set.of("p", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "pre", "li",
            "ul", "ol", "div", "section", "article", "header", "footer", "figure", "img", "video", "table", "hr");
    // Nội dung có một trong các thẻ này thì là HTML của trình soạn thảo; không có thì là chữ thường (dữ liệu mẫu cũ)
    private static final Pattern HTML_TAG = Pattern.compile(
            "<(p|div|h[1-6]|figure|img|video|ul|ol|li|blockquote|pre|table|br|span|strong|b|em|i|a)[\\s>/]", Pattern.CASE_INSENSITIVE);
    // Trong một đoạn chỉ giữ định dạng chữ (in đậm, nghiêng, link...). Script, style, sự kiện on* bị bỏ
    private static final Safelist INLINE = Safelist.basic();
    // Bảng: chỉ giữ thẻ bảng và định dạng chữ
    private static final Safelist TABLE = new Safelist()
            .addTags("table", "caption", "thead", "tbody", "tfoot", "tr", "th", "td", "b", "strong", "i", "em", "u", "br")
            .addAttributes("th", "colspan", "rowspan")
            .addAttributes("td", "colspan", "rowspan");
    // Không tự thêm xuống dòng / thụt lề, để chữ server đếm khớp với chữ trình duyệt hiển thị
    private static final Document.OutputSettings OUTPUT = new Document.OutputSettings().prettyPrint(false);

    public VersionViewDTO toVersionView(ArticleVersion version) {
        List<ContentBlockDTO> blocks = blocks(version.getContent());
        return VersionViewDTO.builder()
                .versionId(version.getId())
                .label(version.getVersionLabel())
                .createdAtText(version.getCreatedAt() != null ? version.getCreatedAt().format(DATE_TIME) : "")
                .changeSummary(version.getChangeSummary())
                .title(normalize(version.getTitle()))
                .subtitle(normalize(version.getSubtitle()))
                .sapo(normalize(version.getSapo()))
                .paragraphs(textsOf(blocks))
                .blocks(blocks)
                .build();
    }

    // replies: các câu trả lời (comment có parent) thuộc comment này
    public ReviewCommentDTO toComment(Comment comment, List<Comment> replies) {
        return ReviewCommentDTO.builder()
                .id(comment.getId())
                .anchorLabel(anchorLabel(comment.getAnchorField(), comment.getAnchorParagraph()))
                .anchorField(comment.getAnchorField() != null ? comment.getAnchorField().name() : null)
                .anchorParagraph(comment.getAnchorParagraph())
                .anchorStart(comment.getAnchorStart())
                .anchorEnd(comment.getAnchorEnd())
                .quotedText(comment.getQuotedText())
                .content(comment.getContent())
                .authorName(comment.getCreator().getFullName())
                .timeText(comment.getCreatedAt() != null ? comment.getCreatedAt().format(TIME) : "")
                .resolved(comment.getStatus() == CommentStatus.RESOLVED)
                // Phiên bản chứa góp ý (comment -> lượt duyệt -> version), ví dụ "1.0"
                .versionLabel(comment.getReview().getVersion().getVersionLabel())
                .replies(replies.stream().map(reply -> toComment(reply, List.of())).toList())
                .build();
    }

    // bodyUrls: link các ảnh / video đang nằm trong thân bài, để đánh dấu tư liệu "Có trong bài"
    public SourceItemDTO toSource(ArticleSource articleSource, Set<String> bodyUrls) {
        Source source = articleSource.getSource();
        String typeLabel = switch (source.getSourceType()) {
            case IMAGE -> "Ảnh";
            case DOCUMENT -> "Tài liệu";
            case WEBSITE -> "Website";
            case INTERVIEW -> "Phỏng vấn";
            case INTERNAL_DOC -> "Tài liệu nội bộ";
        };
        return new SourceItemDTO(source.getTitle(), typeLabel, source.getFileUrl(),
                source.getSourceType() == SourceType.IMAGE,
                articleSource.getCaption(), Boolean.TRUE.equals(articleSource.getFeatured()),
                source.getFileUrl() != null && bodyUrls.contains(source.getFileUrl()));
    }

    // Link của mọi ảnh / video trong thân bài
    public Set<String> mediaUrls(List<ContentBlockDTO> blocks) {
        return blocks.stream()
                .filter(block -> block.getKind() == Kind.IMAGE || block.getKind() == Kind.VIDEO)
                .map(ContentBlockDTO::getUrl)
                .collect(Collectors.toSet());
    }

    // Chữ của từng đoạn, theo đúng cách đánh số đoạn của blocks()
    public List<String> paragraphs(String content) {
        return textsOf(blocks(content));
    }

    // ---------- Tách thân bài thành các khối ----------

    // Thân bài theo đúng thứ tự phóng viên sắp xếp.
    // - Chữ thường (dữ liệu mẫu cũ): các đoạn cách nhau bằng một dòng trống.
    // - HTML từ trình soạn thảo: mỗi <p>, <h2>, <blockquote>, <li>... là một đoạn; <figure><img> là một ảnh...
    public List<ContentBlockDTO> blocks(String content) {
        if (content == null || content.isBlank()) return List.of();
        List<ContentBlockDTO> out = new ArrayList<>();
        if (!HTML_TAG.matcher(content).find()) {
            for (String paragraph : normalize(content).split("\\n\\s*\\n")) {
                String text = paragraph.trim();
                if (!text.isEmpty()) out.add(textBlock("p", HtmlUtils.htmlEscape(text, "UTF-8"), text, out));
            }
            return out;
        }
        Document doc = Jsoup.parseBodyFragment(content);
        doc.outputSettings(OUTPUT);
        collect(doc.body(), "p", out);
        return out;
    }

    // Đi qua các phần tử con theo thứ tự. Chữ và thẻ nằm trong dòng (b, i, a, span...) đứng liền nhau được gom thành một đoạn
    private void collect(Element parent, String textTag, List<ContentBlockDTO> out) {
        StringBuilder loose = new StringBuilder();
        for (Node node : parent.childNodes()) {
            if (node instanceof Element el && BLOCK_TAGS.contains(el.normalName())) {
                addText(textTag, loose.toString(), out);
                loose.setLength(0);
                handleBlock(el, out);
            } else {
                loose.append(node.outerHtml());
            }
        }
        addText(textTag, loose.toString(), out);
    }

    private void handleBlock(Element el, List<ContentBlockDTO> out) {
        String tag = el.normalName();
        // Đoạn chữ không chứa khối nào khác bên trong: là một đoạn
        if (TEXT_TAGS.contains(tag) && !hasBlockChild(el)) {
            addText(tag, el.html(), out);
            return;
        }
        switch (tag) {
            case "figure" -> {
                Element img = el.selectFirst("img");
                Element video = el.selectFirst("video");
                Element figcaption = el.selectFirst("figcaption");
                String caption = figcaption != null ? figcaption.text() : null;
                if (img != null) addMedia(Kind.IMAGE, img.attr("src"), caption != null ? caption : img.attr("alt"), out);
                else if (video != null) addMedia(Kind.VIDEO, video.attr("src"), caption, out);
                else collect(el, "p", out);
            }
            case "img" -> addMedia(Kind.IMAGE, el.attr("src"), el.attr("alt"), out);
            case "video" -> addMedia(Kind.VIDEO, el.attr("src"), null, out);
            case "table" -> out.add(ContentBlockDTO.builder()
                    .kind(Kind.TABLE)
                    .html(Jsoup.clean(el.outerHtml(), "", TABLE, OUTPUT))
                    .build());
            case "hr" -> { /* đường kẻ ngang: không hiển thị */ }
            // div, ul, ol..., hoặc đoạn chữ có ảnh / khối lồng bên trong: đi vào bên trong
            default -> collect(el, TEXT_TAGS.contains(tag) ? tag : "p", out);
        }
    }

    private static boolean hasBlockChild(Element el) {
        return el.children().stream().anyMatch(child -> BLOCK_TAGS.contains(child.normalName()));
    }

    // Thêm một đoạn chữ. Bỏ qua đoạn rỗng (ví dụ <p><br></p> trình soạn thảo chèn sau ảnh)
    private void addText(String tag, String html, List<ContentBlockDTO> out) {
        if (html == null || html.isBlank()) return;
        String clean = Jsoup.clean(html, "", INLINE, OUTPUT);
        // Lấy chữ từ chính bản đã lọc: đây đúng là chữ trình duyệt sẽ hiện, nên vị trí ký tự server tính khớp với JS
        String text = Jsoup.parseBodyFragment(clean).body().wholeText();
        if (text.isBlank()) return;
        out.add(textBlock(tag, clean, text, out));
    }

    private static ContentBlockDTO textBlock(String tag, String html, String text, List<ContentBlockDTO> out) {
        int paragraph = (int) out.stream().filter(block -> block.getKind() == Kind.TEXT).count() + 1;
        return ContentBlockDTO.builder().kind(Kind.TEXT).tag(tag).paragraph(paragraph).html(html).text(text).build();
    }

    // Chỉ nhận link https (link Cloudinary); link lạ như "javascript:" bị bỏ
    private static void addMedia(Kind kind, String url, String caption, List<ContentBlockDTO> out) {
        if (url == null || !url.trim().toLowerCase().startsWith("https://")) return;
        String text = caption == null || caption.isBlank() ? null : caption.trim();
        out.add(ContentBlockDTO.builder().kind(kind).url(url.trim()).caption(text).build());
    }

    private static List<String> textsOf(List<ContentBlockDTO> blocks) {
        return blocks.stream().filter(block -> block.getKind() == Kind.TEXT).map(ContentBlockDTO::getText).toList();
    }

    // ---------- Vị trí góp ý ----------

    // Đoạn văn mà comment trỏ tới (dùng để kiểm tra vị trí và lấy quoted_text); null nếu không tồn tại
    public String anchorText(ArticleVersion version, AnchorField field, int paragraph) {
        if (field == AnchorField.CONTENT) {
            List<String> list = paragraphs(version.getContent());
            return paragraph >= 1 && paragraph <= list.size() ? list.get(paragraph - 1) : null;
        }
        if (paragraph != 1) return null; // tít / tít phụ / sapo chỉ có một đoạn
        return switch (field) {
            case TITLE -> normalize(version.getTitle());
            case SUBTITLE -> normalize(version.getSubtitle());
            case SAPO -> normalize(version.getSapo());
            default -> null;
        };
    }

    public static String anchorLabel(AnchorField field, Integer paragraph) {
        if (field == null) return "Góp ý chung";
        return switch (field) {
            case TITLE -> "Tít";
            case SUBTITLE -> "Tít phụ";
            case SAPO -> "Sapo";
            case CONTENT -> "Đoạn " + paragraph;
        };
    }

    // Trình duyệt đổi \r\n thành \n khi hiển thị, nên phía server cũng đổi để vị trí ký tự khớp với JS
    private static String normalize(String text) {
        return text == null ? null : text.replace("\r\n", "\n").trim();
    }
}
