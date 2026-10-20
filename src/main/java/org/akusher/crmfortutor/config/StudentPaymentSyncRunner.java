package org.akusher.crmfortutor.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.repository.StudentProfileRepository;
import org.akusher.crmfortutor.service.StudentService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StudentPaymentSyncRunner implements ApplicationRunner {

    private final StudentProfileRepository studentProfileRepository;
    private final StudentService studentService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<StudentProfile> students = studentProfileRepository.findAll();
        int syncedCount = 0;
        for (StudentProfile student : students) {
            if (student.getHourlyRate() != null && student.getHourlyRate().compareTo(BigDecimal.ZERO) > 0) {
                studentService.syncStudentPaymentsAndBalance(student, student.getHourlyRate());
                studentProfileRepository.save(student);
                syncedCount++;
            }
        }
        if (syncedCount > 0) {
            log.info("Synchronized payments and balances for {} students with active hourly rates", syncedCount);
        }
    }
}
