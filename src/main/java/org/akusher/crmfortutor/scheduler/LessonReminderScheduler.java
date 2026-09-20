package org.akusher.crmfortutor.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.akusher.crmfortutor.entity.Lesson;
import org.akusher.crmfortutor.entity.LessonStatus;
import org.akusher.crmfortutor.repository.LessonRepository;
import org.akusher.crmfortutor.service.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LessonReminderScheduler {

    private final LessonRepository lessonRepository;
    private final NotificationService notificationService;

    @Value("${reminders.interval-minutes:30}")
    private int intervalMinutes = 30;

    @Scheduled(cron = "${reminders.cron:0 */30 * * * *}")
    public void sendScheduledLessonReminders() {
        log.debug("Running scheduled lesson reminder job...");
        int processedCount = processRemindersForTime(LocalDateTime.now(), intervalMinutes);
        log.debug("Completed scheduled lesson reminder job. Processed {} reminders.", processedCount);
    }

    public int processRemindersForTime(LocalDateTime now, int interval) {
        LocalDateTime target = now.plusHours(24);
        LocalDateTime windowStart = target.minusMinutes(interval);
        LocalDateTime windowEnd = target.plusMinutes(interval);

        List<Lesson> lessons = lessonRepository.findScheduledLessonsForReminder(
                LessonStatus.SCHEDULED, windowStart, windowEnd);

        log.info("Found {} lessons for 24h reminders in window [{} - {}]",
                lessons.size(), windowStart, windowEnd);

        int count = 0;
        for (Lesson lesson : lessons) {
            try {
                boolean sent = notificationService.sendLessonReminder(lesson);
                if (sent) {
                    lesson.setReminderSentAt(Instant.now());
                    lessonRepository.save(lesson);
                    count++;
                    log.info("Successfully sent reminder for lesson id: {}", lesson.getId());
                } else {
                    log.warn("Failed to send reminder for lesson id {}. reminderSentAt left null for retry in next run.", lesson.getId());
                }
            } catch (Exception e) {
                log.error("Failed to send reminder for lesson id {}: {}", lesson.getId(), e.getMessage(), e);
            }
        }
        return count;
    }
}
