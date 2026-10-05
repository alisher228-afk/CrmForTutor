package org.akusher.crmfortutor.repository;

import org.akusher.crmfortutor.entity.TestEntity;
import org.akusher.crmfortutor.entity.TestTargetType;
import org.akusher.crmfortutor.entity.TestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestRepository extends JpaRepository<TestEntity, Long> {

    Optional<TestEntity> findByIdAndTutorId(Long id, Long tutorId);

    List<TestEntity> findByTutorIdOrderByCreatedAtDesc(Long tutorId);

    @Query("""
        SELECT t FROM TestEntity t
        WHERE t.tutor.id = :tutorId
          AND (:type IS NULL OR t.type = :type)
          AND (:targetType IS NULL OR t.targetType = :targetType)
          AND (:groupName IS NULL OR :groupName = '' OR t.groupName = :groupName)
          AND (:studentId IS NULL OR (t.student IS NOT NULL AND t.student.id = :studentId))
          AND (:search IS NULL OR :search = '' OR
               LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(coalesce(t.topic, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(coalesce(t.description, '')) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY t.createdAt DESC
    """)
    Page<TestEntity> searchTests(
            @Param("tutorId") Long tutorId,
            @Param("search") String search,
            @Param("type") TestType type,
            @Param("targetType") TestTargetType targetType,
            @Param("groupName") String groupName,
            @Param("studentId") Long studentId,
            Pageable pageable);

    @Query("""
        SELECT t FROM TestEntity t
        WHERE t.tutor.id = :tutorId
          AND (:type IS NULL OR t.type = :type)
          AND (:search IS NULL OR :search = '' OR
               LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(coalesce(t.topic, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(coalesce(t.description, '')) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY t.createdAt DESC
    """)
    List<TestEntity> findByTutorIdAndFilters(
            @Param("tutorId") Long tutorId,
            @Param("search") String search,
            @Param("type") TestType type);

    @Query("""
        SELECT t FROM TestEntity t
        WHERE t.tutor.id = :tutorId
          AND (
            t.targetType IS NULL OR
            t.targetType = org.akusher.crmfortutor.entity.TestTargetType.ALL OR
            (t.targetType = org.akusher.crmfortutor.entity.TestTargetType.GROUP AND :groupName IS NOT NULL AND :groupName <> '' AND LOWER(t.groupName) = LOWER(CAST(:groupName AS string))) OR
            (t.targetType = org.akusher.crmfortutor.entity.TestTargetType.INDIVIDUAL AND t.student.id = :studentId)
          )
          AND (:type IS NULL OR t.type = :type)
          AND (:search IS NULL OR :search = '' OR
               LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(coalesce(t.topic, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(coalesce(t.description, '')) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY t.createdAt DESC
    """)
    List<TestEntity> findTestsForStudent(
            @Param("tutorId") Long tutorId,
            @Param("studentId") Long studentId,
            @Param("groupName") String groupName,
            @Param("search") String search,
            @Param("type") TestType type);
}
