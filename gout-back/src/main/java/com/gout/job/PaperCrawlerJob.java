package com.gout.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.gout.dao.PaperRepository;
import com.gout.entity.Paper;
import com.gout.service.PaperAiService;
import com.gout.service.PaperEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * PubMed E-utilities 기반 통풍 관련 논문 크롤러.
 * - esearch 로 PMID 수집 (최근 30일, gout OR hyperuricemia)
 * - efetch 로 abstract/metadata 수집
 * - Paper 저장 후 AI 요약 + pgvector 임베딩
 *
 * app.crawler.enabled=true 일 때만 활성화.
 * 매일 03:07 (off-peak) 자동 실행.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.crawler", name = "enabled", havingValue = "true")
public class PaperCrawlerJob {

    private static final String ESEARCH_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi";
    private static final String EFETCH_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi";
    private static final String DEFAULT_QUERY = "gout OR hyperuricemia";
    private static final int DEFAULT_RETMAX = 20;
    private static final int DEFAULT_RELDATE_DAYS = 30;

    private final RestTemplate restTemplate;
    private final PaperRepository paperRepository;
    private final PaperAiService paperAiService;
    private final PaperEmbeddingService paperEmbeddingService;

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final XmlMapper xmlMapper = new XmlMapper();

    /**
     * 매일 03:07 자동 실행. off-peak 시간대.
     */
    @Scheduled(cron = "0 7 3 * * *")
    public void scheduledRun() {
        log.info("Scheduled PubMed crawler run starting");
        run();
    }

    /**
     * 실제 크롤링. 관리자 트리거에서도 호출됨.
     * @return 신규 저장된 논문 수
     */
    public int run() {
        List<String> pmids = searchPmids(DEFAULT_QUERY, DEFAULT_RETMAX, DEFAULT_RELDATE_DAYS);
        if (pmids.isEmpty()) {
            log.info("PubMed esearch returned 0 pmids");
            return 0;
        }
        log.info("PubMed esearch returned {} pmids", pmids.size());

        List<String> newPmids = new ArrayList<>();
        for (String pmid : pmids) {
            if (!paperRepository.existsByPmid(pmid)) {
                newPmids.add(pmid);
            }
        }
        if (newPmids.isEmpty()) {
            log.info("No new pmids to fetch");
            return 0;
        }

        List<Paper> fetched = fetchDetails(newPmids);
        int saved = 0;
        for (Paper p : fetched) {
            if (p.getPmid() != null && paperRepository.existsByPmid(p.getPmid())) continue;
            Paper persisted = paperRepository.save(p);
            saved++;
            enrich(persisted);
        }
        log.info("Crawler saved {} new papers", saved);
        return saved;
    }

    private void enrich(Paper paper) {
        try {
            PaperAiService.SummaryResult summary = paperAiService.summarize(paper.getTitle(), paper.getAbstractEn());
            if (summary != null) {
                paper.updateSummary(summary.abstractKo, summary.aiSummaryKo);
                paperRepository.save(paper);
            }
        } catch (Exception e) {
            log.warn("AI summary failed for {}: {}", paper.getPmid(), e.getMessage());
        }
        try {
            paperEmbeddingService.embedPaper(paper);
        } catch (Exception e) {
            log.warn("Embedding failed for {}: {}", paper.getPmid(), e.getMessage());
        }
    }

    List<String> searchPmids(String query, int retmax, int reldateDays) {
        String url = UriComponentsBuilder.fromUriString(ESEARCH_URL)
                .queryParam("db", "pubmed")
                .queryParam("term", query)
                .queryParam("retmax", retmax)
                .queryParam("retmode", "json")
                .queryParam("reldate", reldateDays)
                .queryParam("datetype", "pdat")
                .build()
                .toUriString();
        try {
            ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) return List.of();
            JsonNode root = jsonMapper.readTree(resp.getBody());
            JsonNode ids = root.path("esearchresult").path("idlist");
            List<String> pmids = new ArrayList<>();
            if (ids.isArray()) {
                for (JsonNode id : ids) {
                    String v = id.asText();
                    if (!v.isBlank()) pmids.add(v);
                }
            }
            return pmids;
        } catch (Exception e) {
            log.warn("esearch failed: {}", e.getMessage());
            return List.of();
        }
    }

    List<Paper> fetchDetails(List<String> pmids) {
        if (pmids == null || pmids.isEmpty()) return List.of();
        String joined = String.join(",", pmids);
        String url = UriComponentsBuilder.fromUriString(EFETCH_URL)
                .queryParam("db", "pubmed")
                .queryParam("id", joined)
                .queryParam("rettype", "abstract")
                .queryParam("retmode", "xml")
                .build()
                .toUriString();
        try {
            ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) return List.of();
            return parseEfetch(resp.getBody());
        } catch (Exception e) {
            log.warn("efetch failed: {}", e.getMessage());
            return List.of();
        }
    }

    List<Paper> parseEfetch(String xml) {
        List<Paper> result = new ArrayList<>();
        try {
            JsonNode root = xmlMapper.readTree(xml.getBytes());
            JsonNode articles = root.path("PubmedArticle");
            if (articles.isMissingNode()) return result;
            if (articles.isArray()) {
                for (JsonNode art : articles) {
                    Paper p = toPaper(art);
                    if (p != null) result.add(p);
                }
            } else {
                Paper p = toPaper(articles);
                if (p != null) result.add(p);
            }
        } catch (Exception e) {
            log.warn("efetch XML parse failed: {}", e.getMessage());
        }
        return result;
    }

    private Paper toPaper(JsonNode article) {
        try {
            JsonNode medline = article.path("MedlineCitation");
            String pmid = medline.path("PMID").path("").asText(
                    medline.path("PMID").asText("")
            );
            if (pmid == null || pmid.isBlank()) {
                // XmlMapper 일부 버전: inner text 가 ""로 매핑될 수도
                pmid = medline.path("PMID").asText("");
            }

            JsonNode articleNode = medline.path("Article");
            String title = asText(articleNode.path("ArticleTitle"));
            String abstractEn = extractAbstract(articleNode.path("Abstract").path("AbstractText"));
            String journalName = asText(articleNode.path("Journal").path("Title"));
            LocalDate publishedAt = parsePubDate(articleNode.path("Journal").path("JournalIssue").path("PubDate"));
            List<String> authors = extractAuthors(articleNode.path("AuthorList").path("Author"));
            String doi = extractDoi(articleNode.path("ELocationID"));
            String sourceUrl = pmid.isBlank() ? null : "https://pubmed.ncbi.nlm.nih.gov/" + pmid + "/";

            if (title == null || title.isBlank()) return null;

            return Paper.builder()
                    .pmid(pmid.isBlank() ? null : pmid)
                    .doi(doi)
                    .title(title)
                    .abstractEn(abstractEn)
                    .authors(authors)
                    .journalName(journalName)
                    .publishedAt(publishedAt)
                    .sourceUrl(sourceUrl)
                    .build();
        } catch (Exception e) {
            log.warn("toPaper failed: {}", e.getMessage());
            return null;
        }
    }

    private String asText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        if (node.isTextual()) return node.asText();
        // 속성 있는 element는 {"":"inner text","-attr":..}로 올 수 있음
        if (node.isObject()) {
            JsonNode inner = node.path("");
            if (inner.isTextual()) return inner.asText();
            return node.asText("");
        }
        return node.asText("");
    }

    private String extractAbstract(JsonNode abstractTextNode) {
        if (abstractTextNode == null || abstractTextNode.isMissingNode()) return null;
        if (abstractTextNode.isTextual()) return abstractTextNode.asText();
        if (abstractTextNode.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode n : abstractTextNode) {
                String t = asText(n);
                if (t != null && !t.isBlank()) {
                    if (sb.length() > 0) sb.append("\n\n");
                    sb.append(t);
                }
            }
            return sb.length() == 0 ? null : sb.toString();
        }
        return asText(abstractTextNode);
    }

    private List<String> extractAuthors(JsonNode authorNode) {
        List<String> authors = new ArrayList<>();
        if (authorNode == null || authorNode.isMissingNode()) return authors;
        if (authorNode.isArray()) {
            for (JsonNode a : authorNode) {
                String name = buildAuthorName(a);
                if (name != null) authors.add(name);
            }
        } else if (authorNode.isObject()) {
            String name = buildAuthorName(authorNode);
            if (name != null) authors.add(name);
        }
        return authors;
    }

    private String buildAuthorName(JsonNode a) {
        String last = asText(a.path("LastName"));
        String fore = asText(a.path("ForeName"));
        String coll = asText(a.path("CollectiveName"));
        if (last != null && !last.isBlank()) {
            if (fore != null && !fore.isBlank()) return fore + " " + last;
            return last;
        }
        return coll;
    }

    private String extractDoi(JsonNode eLocIdNode) {
        if (eLocIdNode == null || eLocIdNode.isMissingNode()) return null;
        if (eLocIdNode.isArray()) {
            for (JsonNode n : eLocIdNode) {
                String doi = doiFromNode(n);
                if (doi != null) return doi;
            }
        } else {
            return doiFromNode(eLocIdNode);
        }
        return null;
    }

    private String doiFromNode(JsonNode n) {
        if (n == null || !n.isObject()) return null;
        String type = n.path("EIdType").asText("");
        if (!"doi".equalsIgnoreCase(type)) {
            // attribute 형태는 "-EIdType"로 올 수도
            String attrType = n.path("EIdType").asText("");
            if (!"doi".equalsIgnoreCase(attrType)) return null;
        }
        String text = n.path("").asText("");
        if (!text.isBlank()) return text;
        return n.asText("");
    }

    LocalDate parsePubDate(JsonNode pubDate) {
        if (pubDate == null || pubDate.isMissingNode()) return null;
        String year = asText(pubDate.path("Year"));
        String month = asText(pubDate.path("Month"));
        String day = asText(pubDate.path("Day"));
        String medlineDate = asText(pubDate.path("MedlineDate"));

        try {
            if (year != null && !year.isBlank()) {
                int y = Integer.parseInt(year.trim());
                int m = parseMonth(month);
                int d = 1;
                if (day != null && !day.isBlank()) {
                    try { d = Integer.parseInt(day.trim()); } catch (NumberFormatException ignore) {}
                }
                return LocalDate.of(y, m, Math.max(1, Math.min(d, 28)));
            }
            if (medlineDate != null && !medlineDate.isBlank()) {
                String first = medlineDate.trim().split("\\s+")[0];
                try {
                    return LocalDate.parse(first, DateTimeFormatter.ofPattern("yyyy"));
                } catch (Exception ignore) {
                    // "2024 Jan" 같은 포맷은 위에서 이미 시도됨
                }
            }
        } catch (Exception e) {
            log.debug("parsePubDate failed: {}", e.getMessage());
        }
        return null;
    }

    private int parseMonth(String month) {
        if (month == null || month.isBlank()) return 1;
        String m = month.trim();
        try {
            return Integer.parseInt(m);
        } catch (NumberFormatException ignore) {
            try {
                return Month.valueOf(m.substring(0, Math.min(3, m.length())).toUpperCase(Locale.ROOT)).getValue();
            } catch (Exception e) {
                return 1;
            }
        }
    }
}
