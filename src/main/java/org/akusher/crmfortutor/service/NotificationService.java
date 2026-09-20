package org.akusher.crmfortutor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.akusher.crmfortutor.entity.Lesson;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final TelegramService telegramService;

    public void sendLessonReminder(Lesson lesson) {
        if (lesson == null) {
            log.warn("Cannot send lesson reminder: lesson is null");
            return;
        }

        StudentProfile student = lesson.getStudent();
        if (student == null) {
            log.warn("Cannot send lesson reminder for lesson id {}: student is null", lesson.getId());
            return;
        }

        String time = lesson.getStartTime() != null ? lesson.getStartTime().format(TIME_FORMATTER) : "";
        String message = buildReminderMessage(lesson, time);

        Long chatId = student.getTelegramChatId();
        if (chatId != null) {
            telegramService.sendMessage(chatId, message);
            log.info("Sent lesson reminder to student id {} (chatId {}) for lesson id {}",
                    student.getId(), chatId, lesson.getId());
        } else {
            log.info("Student id {} has no telegramChatId, reminder skipped for lesson id {}",
                    student.getId(), lesson.getId());
        }
    }

    private String buildReminderMessage(Lesson lesson, String time) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(lesson.getTopic())) {
            sb.append(String.format("Завтра в %s у тебя урок по теме %s", time, lesson.getTopic().trim()));
        } else {
            sb.append(String.format("Завтра в %s у тебя урок", time));
        }

        if (StringUtils.hasText(lesson.getMeetingUrl())) {
            sb.append("\nСсылка на звонок: ").append(lesson.getMeetingUrl().trim());
        }
        return sb.toString();
    }
}
