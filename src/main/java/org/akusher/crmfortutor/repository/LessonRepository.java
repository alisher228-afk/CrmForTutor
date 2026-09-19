package org.akusher.crmfortutor.repository;

import org.akusher.crmfortutor.entity.Lesson;
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
}
