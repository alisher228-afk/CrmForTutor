package org.akusher.crmfortutor.repository;

import org.akusher.crmfortutor.entity.Homework;
import org.akusher.crmfortutor.entity.HomeworkStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HomeworkRepository extends JpaRepository<Homework, Long> {

    @Query("""
        SELECT h FROM Homework h
        WHERE h.id = :id
          AND h.lesson.tutor.id = :tutorId
    """)
    Optional<Homework> findByIdAndTutorId(
            @Param("id") Long id,
            @Param("tutorId") Long tutorId);

    @Query("""
        SELECT h FROM Homework h
        WHERE h.lesson.student.id = :studentId
          AND h.lesson.tutor.id = :tutorId
        ORDER BY h.deadline ASC NULLS LAST, h.id DESC
    """)
    List<Homework> findByStudentIdAndTutorId(
            @Param("studentId") Long studentId,
            @Param("tutorId") Long tutorId);

    @Query(value = """
        SELECT h FROM Homework h
        WHERE h.lesson.student.id = :studentId
          AND h.lesson.tutor.id = :tutorId
          AND (:status IS NULL OR h.status = :status)
          AND (:search IS NULL OR :search = '' OR LOWER(h.title) LIKE LOWER(CONCAT('%', :search, '%')))
    """, countQuery = """
        SELECT count(h) FROM Homework h
        WHERE h.lesson.student.id = :studentId
          AND h.lesson.tutor.id = :tutorId
          AND (:status IS NULL OR h.status = :status)
          AND (:search IS NULL OR :search = '' OR LOWER(h.title) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<Homework> findByStudentIdAndTutorIdWithFilters(
            @Param("studentId") Long studentId,
            @Param("tutorId") Long tutorId,
            @Param("status") HomeworkStatus status,
            @Param("search") String search,
            Pageable pageable);

    @Query("""
        SELECT h FROM Homework h
        WHERE h.lesson.student.id = :studentId
          AND h.lesson.tutor.id = :tutorId
          AND (:status IS NULL OR h.status = :status)
          AND (:search IS NULL OR :search = '' OR LOWER(h.title) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY h.deadline ASC NULLS LAST, h.id DESC
    """)
    List<Homework> findByStudentIdAndTutorIdWithFiltersList(
            @Param("studentId") Long studentId,
            @Param("tutorId") Long tutorId,
            @Param("status") HomeworkStatus status,
            @Param("search") String search);

    @Query("""
        SELECT h.status, COUNT(h)
        FROM Homework h
        WHERE h.lesson.student.id = :studentId
          AND h.lesson.tutor.id = :tutorId
        GROUP BY h.status
    """)
    List<Object[]> countByStatusForStudentAndTutor(
            @Param("studentId") Long studentId,
            @Param("tutorId") Long tutorId);

    @Query("""
        SELECT h FROM Homework h
        WHERE h.lesson.student.id = :studentId
        ORDER BY h.deadline ASC NULLS LAST, h.id DESC
    """)
    List<Homework> findByStudentId(@Param("studentId") Long studentId);

    @Query(value = """
        SELECT h FROM Homework h
        WHERE h.lesson.student.id = :studentId
          AND (:status IS NULL OR h.status = :status)
          AND (:search IS NULL OR :search = '' OR LOWER(h.title) LIKE LOWER(CONCAT('%', :search, '%')))
    """, countQuery = """
        SELECT count(h) FROM Homework h
        WHERE h.lesson.student.id = :studentId
          AND (:status IS NULL OR h.status = :status)
          AND (:search IS NULL OR :search = '' OR LOWER(h.title) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<Homework> findByStudentIdWithFilters(
            @Param("studentId") Long studentId,
            @Param("status") HomeworkStatus status,
            @Param("search") String search,
            Pageable pageable);

    @Query("""
        SELECT h FROM Homework h
        WHERE h.lesson.student.id = :studentId
          AND (:status IS NULL OR h.status = :status)
          AND (:search IS NULL OR :search = '' OR LOWER(h.title) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY h.deadline ASC NULLS LAST, h.id DESC
    """)
    List<Homework> findByStudentIdWithFiltersList(
            @Param("studentId") Long studentId,
            @Param("status") HomeworkStatus status,
            @Param("search") String search);

    @Query("""
        SELECT h.status, COUNT(h)
        FROM Homework h
        WHERE h.lesson.student.id = :studentId
        GROUP BY h.status
    """)
    List<Object[]> countByStatusForStudent(
            @Param("studentId") Long studentId);

    List<Homework> findByLessonId(Long lessonId);

    @Query("""
        SELECT h FROM Homework h
        WHERE h.lesson.tutor.id = :tutorId
          AND (h.lesson.groupName = :groupName OR (h.lesson.student.groupName = :groupName AND (h.lesson.groupName IS NULL OR h.lesson.groupName = '')))
        ORDER BY h.deadline ASC NULLS LAST, h.id DESC
    """)
    List<Homework> findByGroupNameAndTutorId(
            @Param("groupName") String groupName,
            @Param("tutorId") Long tutorId);
}
