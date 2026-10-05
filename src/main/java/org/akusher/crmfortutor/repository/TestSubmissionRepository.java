package org.akusher.crmfortutor.repository;

import org.akusher.crmfortutor.entity.TestSubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestSubmissionRepository extends JpaRepository<TestSubmissionEntity, Long> {

    List<TestSubmissionEntity> findByStudentId(Long studentId);

    List<TestSubmissionEntity> findByTestIdOrderBySubmittedAtDesc(Long testId);

    Optional<TestSubmissionEntity> findTopByTestIdAndStudentIdOrderBySubmittedAtDesc(Long testId, Long studentId);

    @Query("SELECT ts FROM TestSubmissionEntity ts WHERE ts.student.id = :studentId AND ts.test.id IN :testIds")
    List<TestSubmissionEntity> findByStudentIdAndTestIds(@Param("studentId") Long studentId, @Param("testIds") List<Long> testIds);

    long countByTestId(Long testId);
}
