package org.akusher.crmfortutor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CrmForTutorApplicationTests {

    @org.springframework.beans.factory.annotation.Autowired
    private org.akusher.crmfortutor.repository.LessonRepository lessonRepository;

    @Test
    void contextLoads() {
        lessonRepository.findByTutorIdAndFilters(1L, null, null, null);
        lessonRepository.findByTutorIdAndFilters(1L, 2L, java.time.LocalDateTime.now(), java.time.LocalDateTime.now().plusDays(7));
        lessonRepository.findByStudentIdAndFilters(1L, null, null);
        lessonRepository.findByStudentIdAndFilters(1L, java.time.LocalDateTime.now(), java.time.LocalDateTime.now().plusDays(7));
    }
}
