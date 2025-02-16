package at.fhtw.rest.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Getter
@Setter
@SuppressWarnings("JpaDataSourceORMInspection")
public class DocumentEntity {
    @Id
    @Column(name = "doc_id", nullable = false)
    private String id;
    @Column(name = "filename")
    private String filename;
    @Column(name = "filesize")
    private long filesize;
    @Column(name = "filetype")
    private String filetype;
    @Column(name = "object_key")
    private String objectKey;
    @Column(name = "upload_date")
    private LocalDateTime uploadDate;
    @Column(name = "ocr_job_done")
    private boolean ocrJobDone;
    @Column(name = "ocr_text", columnDefinition = "TEXT")
    private String ocrText;
}