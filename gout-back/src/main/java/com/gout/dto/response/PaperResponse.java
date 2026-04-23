package com.gout.dto.response;

import com.gout.entity.Paper;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class PaperResponse {

    private final String id;
    private final String pmid;
    private final String doi;
    private final String title;
    private final String abstractKo;
    private final String aiSummaryKo;
    private final List<String> authors;
    private final String journalName;
    private final LocalDate publishedAt;
    private final String sourceUrl;
    private final String category;

    private PaperResponse(String id,
                          String pmid,
                          String doi,
                          String title,
                          String abstractKo,
                          String aiSummaryKo,
                          List<String> authors,
                          String journalName,
                          LocalDate publishedAt,
                          String sourceUrl,
                          String category) {
        this.id = id;
        this.pmid = pmid;
        this.doi = doi;
        this.title = title;
        this.abstractKo = abstractKo;
        this.aiSummaryKo = aiSummaryKo;
        this.authors = authors;
        this.journalName = journalName;
        this.publishedAt = publishedAt;
        this.sourceUrl = sourceUrl;
        this.category = category;
    }

    public static PaperResponse of(Paper p) {
        return new PaperResponse(
                p.getId(),
                p.getPmid(),
                p.getDoi(),
                p.getTitle(),
                p.getAbstractKo(),
                p.getAiSummaryKo(),
                p.getAuthors(),
                p.getJournalName(),
                p.getPublishedAt(),
                p.getSourceUrl(),
                p.getCategory()
        );
    }
}
