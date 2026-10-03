package org.akusher.crmfortutor.repository;

import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.entity.StudentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    Optional<StudentProfile> findByIdAndTutorId(Long id, Long tutorId);

    Optional<StudentProfile> findByInviteToken(String inviteToken);

    Optional<StudentProfile> findByUserId(Long userId);
 
    Optional<StudentProfile> findByTelegramLinkCode(String telegramLinkCode);

    @Query("""
        SELECT s FROM StudentProfile s
        WHERE s.tutor.id = :tutorId
          AND s.groupName = :groupName
          AND s.status = org.akusher.crmfortutor.entity.StudentStatus.ACTIVE
        ORDER BY s.firstName ASC, s.lastName ASC
    """)
    List<StudentProfile> findActiveByTutorIdAndGroupName(
            @Param("tutorId") Long tutorId,
            @Param("groupName") String groupName);

    @Query("""
        SELECT s FROM StudentProfile s
        WHERE s.tutor.id = :tutorId
          AND s.groupName IS NOT NULL
          AND TRIM(s.groupName) <> ''
          AND s.status = org.akusher.crmfortutor.entity.StudentStatus.ACTIVE
        ORDER BY s.groupName ASC, s.firstName ASC, s.lastName ASC
    """)
    List<StudentProfile> findAllActiveGroupStudents(@Param("tutorId") Long tutorId);

    @Query("""
        SELECT s FROM StudentProfile s
        WHERE s.tutor.id = :tutorId
          AND s.status = :status
          AND (
            (:format IS NULL OR :format = '' OR :format = 'ALL') OR
            (:format = 'INDIVIDUAL' AND (s.groupName IS NULL OR TRIM(s.groupName) = '')) OR
            (:format = 'GROUP' AND (s.groupName IS NOT NULL AND TRIM(s.groupName) <> ''))
          )
          AND (:groupName IS NULL OR :groupName = '' OR s.groupName = :groupName)
          AND (:search IS NULL OR :search = '' OR
               LOWER(s.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(s.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(CONCAT(s.firstName, ' ', coalesce(s.lastName, ''))) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<StudentProfile> findActiveStudents(
            @Param("tutorId") Long tutorId,
            @Param("status") StudentStatus status,
            @Param("format") String format,
            @Param("groupName") String groupName,
            @Param("search") String search,
            Pageable pageable);

    @Query("""
        SELECT s FROM StudentProfile s
        WHERE s.tutor.id = :tutorId
          AND s.status = :status
          AND (:search IS NULL OR :search = '' OR
               LOWER(s.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(s.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(CONCAT(s.firstName, ' ', coalesce(s.lastName, ''))) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<StudentProfile> findActiveStudents(
            @Param("tutorId") Long tutorId,
            @Param("status") StudentStatus status,
            @Param("search") String search,
            Pageable pageable);
}

