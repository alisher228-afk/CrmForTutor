package org.akusher.crmfortutor.scheduler;

import org.akusher.crmfortutor.entity.Lesson;
import org.akusher.crmfortutor.entity.LessonStatus;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.repository.LessonRepository;
import org.akusher.crmfortutor.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonReminderSchedulerTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private LessonReminderScheduler scheduler;

    @Test
    @DisplayName("processRemindersForTime - sends reminder and updates reminderSentAt")
    void processReminders_Success() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 20, 12, 0);

        StudentProfile student = StudentProfile.builder()
                .id(1L)
                .firstName("Анна")
                .telegramChatId(12345678L)
                .build();

        Lesson lesson1 = Lesson.builder()
                .id(101L)
                .student(student)
                .startTime(now.plusHours(24))
                .status(LessonStatus.SCHEDULED)
                .topic("Алгебра")
                .build();

        Lesson lesson2 = Lesson.builder()
                .id(102L)
                .student(student)
                .startTime(now.plusHours(24).plusMinutes(15))
                .status(LessonStatus.SCHEDULED)
                .topic("Геометрия")
                .build();

        when(lessonRepository.findScheduledLessonsForReminder(
                eq(LessonStatus.SCHEDULED), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(lesson1, lesson2));

        when(notificationService.sendLessonReminder(lesson1)).thenReturn(true);
        when(notificationService.sendLessonReminder(lesson2)).thenReturn(true);

        int processed = scheduler.processRemindersForTime(now, 30);

        assertThat(processed).isEqualTo(2);
        assertThat(lesson1.getReminderSentAt()).isNotNull();
        assertThat(lesson2.getReminderSentAt()).isNotNull();

        verify(notificationService).sendLessonReminder(lesson1);
        verify(notificationService).sendLessonReminder(lesson2);
        verify(lessonRepository).save(lesson1);
        verify(lessonRepository).save(lesson2);
    }

    @Test
    @DisplayName("processRemindersForTime - single lesson failure does not stop job execution")
    void processReminders_SingleLessonFailure_ContinuesProcessing() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 20, 12, 0);

        StudentProfile student = StudentProfile.builder()
                .id(2L)
                .telegramChatId(888888L)
                .build();

        Lesson failingLesson = Lesson.builder()
                .id(201L)
                .student(student)
                .startTime(now.plusHours(24))
                .status(LessonStatus.SCHEDULED)
                .topic("Ошибка")
                .build();

        Lesson succeedingLesson = Lesson.builder()
                .id(202L)
                .student(student)
                .startTime(now.plusHours(24).plusMinutes(20))
                .status(LessonStatus.SCHEDULED)
                .topic("Успех")
                .build();

        when(lessonRepository.findScheduledLessonsForReminder(
                eq(LessonStatus.SCHEDULED), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(failingLesson, succeedingLesson));

        doThrow(new RuntimeException("Simulated Telegram error"))
                .when(notificationService).sendLessonReminder(failingLesson);

        when(notificationService.sendLessonReminder(succeedingLesson)).thenReturn(true);

        int processed = scheduler.processRemindersForTime(now, 30);

        assertThat(processed).isEqualTo(1);
        assertThat(failingLesson.getReminderSentAt()).isNull();
        assertThat(succeedingLesson.getReminderSentAt()).isNotNull();

        verify(notificationService).sendLessonReminder(failingLesson);
        verify(notificationService).sendLessonReminder(succeedingLesson);
        verify(lessonRepository, times(0)).save(failingLesson);
        verify(lessonRepository, times(1)).save(succeedingLesson);
    }

    @Test
    @DisplayName("processRemindersForTime - неудачная отправка не выставляет reminderSentAt")
    void processReminders_FailedSending_DoesNotSetReminderSentAt() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 20, 12, 0);

        StudentProfile student = StudentProfile.builder()
                .id(3L)
                .telegramChatId(777777L)
                .build();

        Lesson lesson = Lesson.builder()
                .id(301L)
                .student(student)
                .startTime(now.plusHours(24))
                .status(LessonStatus.SCHEDULED)
                .topic("Химия")
                .reminderSentAt(null)
                .build();

        when(lessonRepository.findScheduledLessonsForReminder(
                eq(LessonStatus.SCHEDULED), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(lesson));

        when(notificationService.sendLessonReminder(lesson)).thenReturn(false);

        int processed = scheduler.processRemindersForTime(now, 30);

        assertThat(processed).isEqualTo(0);
        assertThat(lesson.getReminderSentAt()).isNull();

        verify(notificationService).sendLessonReminder(lesson);
        verify(lessonRepository, never()).save(lesson);
    }
}
