package org.akusher.crmfortutor.repository;

import org.akusher.crmfortutor.entity.Lesson;
import org.akusher.crmfortutor.entity.LessonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    Optional<Lesson> findByIdAndTutorId(Long id, Long tutorId);

    @Query("""
        SELECT l FROM Lesson l
        WHERE l.tutor.id = :tutorId
          AND l.startTime >= :from
          AND l.startTime <= :to
        ORDER BY l.startTime ASC
    """)
    List<Lesson> findByTutorIdAndInterval(
            @Param("tutorId") Long tutorId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
        SELECT l FROM Lesson l
        WHERE l.student.id = :studentId
          AND l.startTime >= :from
          AND l.startTime <= :to
        ORDER BY l.startTime ASC
    """)
    List<Lesson> findByStudentIdAndInterval(
            @Param("studentId") Long studentId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
        SELECT l FROM Lesson l
        JOIN FETCH l.student s
        WHERE l.status = :status
          AND l.reminderSentAt IS NULL
          AND l.startTime >= :windowStart
          AND l.startTime <= :windowEnd
        ORDER BY l.startTime ASC
    """)
    List<Lesson> findScheduledLessonsForReminder(
            @Param("status") LessonStatus status,
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd);
}
