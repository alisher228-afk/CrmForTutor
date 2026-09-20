package org.akusher.crmfortutor.repository;

import org.akusher.crmfortutor.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByHomeworkId(Long homeworkId);
}
