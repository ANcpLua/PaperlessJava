package at.fhtw.rest.infrastructure.mapper;

import at.fhtw.rest.api.DocumentRequest;
import at.fhtw.rest.persistence.DocumentEntity;
import at.fhtw.rest.infrastructure.mapper.imp.IDocumentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DocumentMapper implements IDocumentMapper {

    @Override
    public DocumentEntity toEntity(DocumentRequest request) {
        if (request == null) {
            log.warn("[DocumentMapper.toEntity] Received null DocumentRequest");
            return null;
        }
        log.debug("[DocumentMapper.toEntity] Mapping DocumentRequest with id: {}", request.getId());
        DocumentEntity entity = new DocumentEntity();
        entity.setId(request.getId());
        entity.setFilename(request.getFilename());
        entity.setFilesize(request.getFilesize());
        entity.setFiletype(request.getFiletype());
        entity.setUploadDate(request.getUploadDate());
        entity.setOcrJobDone(request.isOcrJobDone());
        entity.setOcrText(request.getOcrText());
        entity.setObjectKey(request.getId());
        log.debug("[DocumentMapper.toEntity] Mapped entity: {}", entity);
        return entity;
    }

    @Override
    public DocumentRequest toDto(DocumentEntity entity) {
        if (entity == null) {
            log.warn("[DocumentMapper.toDto] Received null DocumentEntity");
            return null;
        }
        log.debug("[DocumentMapper.toDto] Mapping DocumentEntity with id: {}", entity.getId());
        DocumentRequest dto = DocumentRequest.builder()
                .id(entity.getId())
                .filename(entity.getFilename())
                .filesize(entity.getFilesize())
                .filetype(entity.getFiletype())
                .uploadDate(entity.getUploadDate())
                .ocrJobDone(entity.isOcrJobDone())
                .ocrText(entity.getOcrText())
                .build();
        log.debug("[DocumentMapper.toDto] Mapped DTO: {}", dto);
        return dto;
    }
}