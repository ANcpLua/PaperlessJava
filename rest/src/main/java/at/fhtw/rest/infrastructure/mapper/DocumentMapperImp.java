package at.fhtw.rest.infrastructure.mapper;

import at.fhtw.rest.api.DocumentRequest;
import at.fhtw.rest.persistence.DocumentEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DocumentMapperImp implements DocumentMapper {

    @Override
    public DocumentEntity toEntity(DocumentRequest request) {
        if (request == null) {
            log.warn("[DocumentMapperImp.toEntity] Received null DocumentRequest");
            return null;
        }
        log.debug("[DocumentMapperImp.toEntity] Mapping DocumentRequest with id: {}", request.getId());
        DocumentEntity entity = new DocumentEntity();
        entity.setId(request.getId());
        entity.setFilename(request.getFilename());
        entity.setFilesize(request.getFilesize());
        entity.setFiletype(request.getFiletype());
        entity.setUploadDate(request.getUploadDate());
        entity.setOcrJobDone(request.isOcrJobDone());
        entity.setOcrText(request.getOcrText());
        entity.setObjectKey(request.getId());
        log.debug("[DocumentMapperImp.toEntity] Mapped entity: {}", entity);
        return entity;
    }

    @Override
    public DocumentRequest toDto(DocumentEntity entity) {
        if (entity == null) {
            log.warn("[DocumentMapperImp.toDto] Received null DocumentEntity");
            return null;
        }
        log.debug("[DocumentMapperImp.toDto] Mapping DocumentEntity with id: {}", entity.getId());
        DocumentRequest dto = DocumentRequest.builder()
                .id(entity.getId())
                .filename(entity.getFilename())
                .filesize(entity.getFilesize())
                .filetype(entity.getFiletype())
                .uploadDate(entity.getUploadDate())
                .ocrJobDone(entity.isOcrJobDone())
                .ocrText(entity.getOcrText())
                .build();
        log.debug("[DocumentMapperImp.toDto] Mapped DTO: {}", dto);
        return dto;
    }
}