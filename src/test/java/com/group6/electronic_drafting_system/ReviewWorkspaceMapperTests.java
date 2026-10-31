package com.group6.electronic_drafting_system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.group6.electronic_drafting_system.dto.DepartmentHead.ContentBlockDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.ContentBlockDTO.Kind;
import com.group6.electronic_drafting_system.entity.ArticleVersion;
import com.group6.electronic_drafting_system.entity.enums.AnchorField;
import com.group6.electronic_drafting_system.mapper.DepartmentHead.ReviewWorkspaceMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

// Màn workspace Trưởng phòng hiển thị thân bài theo đúng bố cục phóng viên sắp xếp
class ReviewWorkspaceMapperTests {
    private final ReviewWorkspaceMapper mapper = new ReviewWorkspaceMapper();

    // HTML giống trình soạn thảo sinh ra (editor.js): đoạn, ảnh chèn giữa bài, đoạn rỗng sau ảnh, tít phụ
    private static final String EDITOR_HTML =
            "<p>Chợ nổi <b>Cái Răng</b> họp từ 5 giờ sáng.</p>"
            + "<figure class=\"article-figure\"><img src=\"https://res.cloudinary.com/demo/cho-noi.jpg\" alt=\"Ghe\">"
            + "<figcaption>Ghe hàng trên sông</figcaption></figure><p><br></p>"
            + "<h2>Giá trái cây</h2>"
            + "<p>Đoạn cuối.</p>";

    @Test
    void plainTextIsSplitByBlankLines() {
        List<ContentBlockDTO> blocks = mapper.blocks("Đoạn một.\n\nĐoạn hai\ncó xuống dòng.");
        assertEquals(2, blocks.size());
        assertEquals(2, blocks.get(1).getParagraph());
        assertEquals("Đoạn hai\ncó xuống dòng.", blocks.get(1).getText());
    }

    @Test
    void editorHtmlKeepsOrderOfTextAndImages() {
        List<ContentBlockDTO> blocks = mapper.blocks(EDITOR_HTML);
        assertEquals(List.of(Kind.TEXT, Kind.IMAGE, Kind.TEXT, Kind.TEXT), blocks.stream().map(ContentBlockDTO::getKind).toList());

        assertEquals("Chợ nổi Cái Răng họp từ 5 giờ sáng.", blocks.get(0).getText());
        assertEquals(1, blocks.get(0).getParagraph());

        assertEquals("https://res.cloudinary.com/demo/cho-noi.jpg", blocks.get(1).getUrl());
        assertEquals("Ghe hàng trên sông", blocks.get(1).getCaption());
        assertNull(blocks.get(1).getParagraph()); // ảnh không được đánh số đoạn

        assertEquals("h2", blocks.get(2).getTag());
        assertEquals(2, blocks.get(2).getParagraph()); // <p><br></p> rỗng bị bỏ, không chiếm số đoạn
        assertEquals(3, blocks.get(3).getParagraph());
    }

    @Test
    void scriptsAndUnsafeLinksAreRemoved() {
        List<ContentBlockDTO> blocks = mapper.blocks(
                "<p>Xin chào<script>alert(1)</script></p><img src=\"javascript:alert(1)\"><p onclick=\"x()\">Hết</p>");
        assertEquals(2, blocks.size());
        assertFalse(blocks.get(0).getHtml().contains("script"));
        assertFalse(blocks.get(1).getHtml().contains("onclick"));
    }

    @Test
    void anchorTextMatchesDisplayedParagraph() {
        ArticleVersion version = new ArticleVersion();
        version.setContent(EDITOR_HTML);
        assertEquals("Đoạn cuối.", mapper.anchorText(version, AnchorField.CONTENT, 3));
        assertNull(mapper.anchorText(version, AnchorField.CONTENT, 4));
        assertEquals(List.of("Chợ nổi Cái Răng họp từ 5 giờ sáng.", "Giá trái cây", "Đoạn cuối."), mapper.paragraphs(EDITOR_HTML));
    }
}
