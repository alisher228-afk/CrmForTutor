package org.akusher.crmfortutor.repository;

import org.akusher.crmfortutor.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("""
        SELECT p FROM Payment p
        WHERE p.id = :id
          AND p.student.tutor.id = :tutorId
    """)
    Optional<Payment> findByIdAndTutorId(
            @Param("id") Long id,
            @Param("tutorId") Long tutorId);

    @Query("""
        SELECT p FROM Payment p
        WHERE p.student.id = :studentId
          AND p.student.tutor.id = :tutorId
        ORDER BY p.paymentDate DESC, p.id DESC
    """)
    List<Payment> findByStudentIdAndTutorId(
            @Param("studentId") Long studentId,
            @Param("tutorId") Long tutorId);

    @Query("""
        SELECT p FROM Payment p
        WHERE p.student.id = :studentId
        ORDER BY p.paymentDate DESC, p.id DESC
    """)
    List<Payment> findByStudentId(@Param("studentId") Long studentId);

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE p.student.tutor.id = :tutorId
          AND p.paymentDate >= :startDate
          AND p.paymentDate <= :endDate
    """)
    BigDecimal calculateIncomeBetween(
            @Param("tutorId") Long tutorId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
