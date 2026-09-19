package org.akusher.crmfortutor.repository;

import org.akusher.crmfortutor.entity.Homework;
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
}
