package at.fhtw.rest.persistence.imp;

import at.fhtw.rest.persistence.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IDocumentRepository extends JpaRepository<DocumentEntity, String> {
}