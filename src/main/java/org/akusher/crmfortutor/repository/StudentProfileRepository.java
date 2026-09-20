package org.akusher.crmfortutor.repository;

import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.entity.StudentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
