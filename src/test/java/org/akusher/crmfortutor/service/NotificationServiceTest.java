package org.akusher.crmfortutor.service;

import org.akusher.crmfortutor.entity.Lesson;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private TelegramService telegramService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("sendLessonReminder - formats message with topic and sends via Telegram")
    void sendLessonReminder_WithTopic_Success() {
        StudentProfile student = StudentProfile.builder()
                .id(1L)
                .firstName("Иван")
                .telegramChatId(11223344L)
                .build();

        LocalDateTime startTime = LocalDateTime.of(2026, 9, 21, 14, 30);
        Lesson lesson = Lesson.builder()
                .id(10L)
                .student(student)
                .startTime(startTime)
                .topic("Математический анализ")
                .build();

        when(telegramService.sendMessage(anyLong(), anyString())).thenReturn(true);

        boolean result = notificationService.sendLessonReminder(lesson);

        ArgumentCaptor<Long> chatIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        verify(telegramService).sendMessage(chatIdCaptor.capture(), messageCaptor.capture());

        assertThat(result).isTrue();
        assertThat(chatIdCaptor.getValue()).isEqualTo(11223344L);
        assertThat(messageCaptor.getValue()).isEqualTo("Завтра в 14:30 у тебя урок по теме Математический анализ");
    }

    @Test
    @DisplayName("sendLessonReminder - formats message without topic")
    void sendLessonReminder_WithoutTopic_Success() {
        StudentProfile student = StudentProfile.builder()
                .id(2L)
                .telegramChatId(55667788L)
                .build();

        LocalDateTime startTime = LocalDateTime.of(2026, 9, 21, 18, 0);
        Lesson lesson = Lesson.builder()
                .id(11L)
                .student(student)
                .startTime(startTime)
                .topic(null)
                .build();

        when(telegramService.sendMessage(anyLong(), anyString())).thenReturn(true);

        boolean result = notificationService.sendLessonReminder(lesson);

        assertThat(result).isTrue();
        verify(telegramService).sendMessage(55667788L, "Завтра в 18:00 у тебя урок");
    }

    @Test
    @DisplayName("sendLessonReminder - includes meetingUrl if present")
    void sendLessonReminder_WithMeetingUrl() {
        StudentProfile student = StudentProfile.builder()
                .id(3L)
                .telegramChatId(998877L)
                .build();

        LocalDateTime startTime = LocalDateTime.of(2026, 9, 21, 10, 15);
        Lesson lesson = Lesson.builder()
                .id(12L)
                .student(student)
                .startTime(startTime)
                .topic("Физика")
                .meetingUrl("https://meet.google.com/abc-def-ghi")
                .build();

        when(telegramService.sendMessage(anyLong(), anyString())).thenReturn(true);

        boolean result = notificationService.sendLessonReminder(lesson);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(telegramService).sendMessage(anyLong(), messageCaptor.capture());

        assertThat(result).isTrue();
        assertThat(messageCaptor.getValue())
                .contains("Завтра в 10:15 у тебя урок по теме Физика")
                .contains("Ссылка на звонок: https://meet.google.com/abc-def-ghi");
    }

    @Test
    @DisplayName("sendLessonReminder - student without telegramChatId skips sending gracefully and returns false")
    void sendLessonReminder_NoTelegramChatId_Skips() {
        StudentProfile student = StudentProfile.builder()
                .id(4L)
                .telegramChatId(null)
                .build();

        Lesson lesson = Lesson.builder()
                .id(13L)
                .student(student)
                .startTime(LocalDateTime.of(2026, 9, 21, 12, 0))
                .topic("Химия")
                .build();

        boolean result = notificationService.sendLessonReminder(lesson);

        assertThat(result).isFalse();
        verify(telegramService, never()).sendMessage(anyLong(), anyString());
    }

    @Test
    @DisplayName("sendLessonReminder - returns false when telegramService.sendMessage fails")
    void sendLessonReminder_TelegramFails_ReturnsFalse() {
        StudentProfile student = StudentProfile.builder()
                .id(5L)
                .telegramChatId(111222333L)
                .build();

        Lesson lesson = Lesson.builder()
                .id(14L)
                .student(student)
                .startTime(LocalDateTime.of(2026, 9, 21, 16, 0))
                .topic("Биология")
                .build();

        when(telegramService.sendMessage(anyLong(), anyString())).thenReturn(false);

        boolean result = notificationService.sendLessonReminder(lesson);

        assertThat(result).isFalse();
        verify(telegramService).sendMessage(111222333L, "Завтра в 16:00 у тебя урок по теме Биология");
    }

    @Test
    @DisplayName("sendLessonReminder - null lesson or student handled safely and returns false")
    void sendLessonReminder_NullArgs() {
        assertThat(notificationService.sendLessonReminder(null)).isFalse();
        verify(telegramService, never()).sendMessage(any(), any());

        Lesson lessonWithoutStudent = Lesson.builder().id(99L).build();
        assertThat(notificationService.sendLessonReminder(lessonWithoutStudent)).isFalse();
        verify(telegramService, never()).sendMessage(any(), any());
    }
}
