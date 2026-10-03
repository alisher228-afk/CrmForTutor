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

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long>, JpaSpecificationExecutor<Lesson> {

    Optional<Lesson> findByIdAndTutorId(Long id, Long tutorId);

    default List<Lesson> findByTutorIdAndFilters(Long tutorId, Long studentId, LocalDateTime from, LocalDateTime to) {
        Specification<Lesson> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tutor").get("id"), tutorId));
            if (studentId != null) {
                predicates.add(cb.equal(root.get("student").get("id"), studentId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startTime"), to));
            }
            query.orderBy(cb.asc(root.get("startTime")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(spec);
    }

    default List<Lesson> findByStudentIdAndFilters(Long studentId, LocalDateTime from, LocalDateTime to) {
        Specification<Lesson> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("student").get("id"), studentId));
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startTime"), to));
            }
            query.orderBy(cb.asc(root.get("startTime")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(spec);
    }

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

    @Query("""
        SELECT COUNT(l) > 0 FROM Lesson l
        WHERE l.student.id = :studentId
          AND l.status NOT IN (org.akusher.crmfortutor.entity.LessonStatus.CANCELLED_BY_STUDENT, org.akusher.crmfortutor.entity.LessonStatus.CANCELLED_BY_TUTOR)
          AND l.startTime < :end
          AND l.endTime > :start
          AND (:excludeId IS NULL OR l.id <> :excludeId)
    """)
    boolean existsConflictingLesson(
            @Param("studentId") Long studentId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("excludeId") Long excludeId);
}
