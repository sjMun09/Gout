package com.gout.entity;

import com.gout.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "papers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Paper extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(length = 20, unique = true)
    private String pmid;

    @Column(length = 300, unique = true)
    private String doi;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "abstract_en", columnDefinition = "TEXT")
    private String abstractEn;

    @Column(name = "abstract_ko", columnDefinition = "TEXT")
    private String abstractKo;

    @Column(name = "ai_summary_ko", columnDefinition = "TEXT")
    private String aiSummaryKo;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "authors", columnDefinition = "text[]")
    private List<String> authors;

    @Column(name = "journal_name", length = 500)
    private String journalName;

    @Column(name = "published_at")
    private LocalDate publishedAt;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(length = 100)
    private String category;

    @Builder
    private Paper(String pmid,
                  String doi,
                  String title,
                  String abstractEn,
                  String abstractKo,
                  String aiSummaryKo,
                  List<String> authors,
                  String journalName,
                  LocalDate publishedAt,
                  String sourceUrl,
                  String category) {
        this.pmid = pmid;
        this.doi = doi;
        this.title = title;
        this.abstractEn = abstractEn;
        this.abstractKo = abstractKo;
        this.aiSummaryKo = aiSummaryKo;
        this.authors = authors;
        this.journalName = journalName;
        this.publishedAt = publishedAt;
        this.sourceUrl = sourceUrl;
        this.category = category;
    }
}
