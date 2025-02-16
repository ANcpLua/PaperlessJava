package at.fhtw.rest.infrastructure.mapper.imp;

import at.fhtw.rest.api.DocumentRequest;
import at.fhtw.rest.persistence.DocumentEntity;

public interface IDocumentMapper {
    DocumentEntity toEntity(DocumentRequest request);
    DocumentRequest toDto(DocumentEntity entity);
}