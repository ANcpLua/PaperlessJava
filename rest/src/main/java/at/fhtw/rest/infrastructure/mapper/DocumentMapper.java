package at.fhtw.rest.infrastructure.mapper;

import at.fhtw.rest.api.DocumentRequest;
import at.fhtw.rest.persistence.DocumentEntity;

public interface DocumentMapper {
    DocumentEntity toEntity(DocumentRequest request);
    DocumentRequest toDto(DocumentEntity entity);
}