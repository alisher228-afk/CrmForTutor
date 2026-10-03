package org.akusher.crmfortutor.repository;

import org.akusher.crmfortutor.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    @Query("SELECT a FROM Attachment a WHERE a.homework.id = :homeworkId")
    List<Attachment> findByHomeworkId(@Param("homeworkId") Long homeworkId);

    @Query("SELECT a FROM Attachment a WHERE a.homework.id IN :homeworkIds")
    List<Attachment> findByHomeworkIdIn(@Param("homeworkIds") List<Long> homeworkIds);
}
