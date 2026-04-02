package com.samhcoco.ai.tools.claude.invoicetool.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@EqualsAndHashCode
@Entity
@Table(name = "invoice")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_path", nullable = false, length = 1024)
    private String filePath;

    /** SHA-256 hex digest of the raw file bytes — unique per document. */
    @Column(name = "content_hash", nullable = false, unique = true, length = 64)
    private String contentHash;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;
}
