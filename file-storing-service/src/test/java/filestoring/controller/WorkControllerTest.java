package filestoring.controller;

import filestoring.domain.Work;
import filestoring.dto.FileData;
import filestoring.exception.FileStoringException;
import filestoring.service.WorkService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkController.class)
class WorkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkService workService;

    @Test
    void shouldUploadWork() throws Exception {
        Work work = new Work();
        work.setId(1L);
        work.setStudentName("Иван");
        work.setOriginalFileName("test.txt");

        when(workService.saveWork(anyString(), any())).thenReturn(work);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "content".getBytes()
        );

        mockMvc.perform(multipart("/api/works")
                        .file(file)
                        .param("studentName", "Иван"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.originalFileName").value("test.txt"));
    }

    @Test
    void shouldDownloadFile() throws Exception {
        FileData fileData = new FileData(
                new ByteArrayInputStream("data".getBytes()),
                "test.txt",
                "text/plain",
                4L
        );
        when(workService.getWorkFile(1L)).thenReturn(fileData);

        mockMvc.perform(get("/api/works/1/file"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/plain"))
                .andExpect(content().bytes("data".getBytes()));
    }

    @Test
    void shouldGetWorkById() throws Exception {
        Work work = new Work();
        work.setId(1L);
        work.setStudentName("Иван");
        work.setOriginalFileName("test.txt");

        when(workService.getWorkById(1L)).thenReturn(work);

        mockMvc.perform(get("/api/works/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.originalFileName").value("test.txt"));
    }

    @Test
    @DisplayName("Загрузка: 400 при пустом имени студента")
    void uploadWork_shouldReturn400_whenStudentNameBlank() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "content".getBytes());

        mockMvc.perform(multipart("/api/works")
                        .file(file)
                        .param("studentName", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Получение работы: 404 когда не найдена")
    void getWork_shouldReturn404_whenWorkNotFound() throws Exception {
        when(workService.getWorkById(99L)).thenThrow(new FileStoringException("Работа не найдена"));

        mockMvc.perform(get("/api/works/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Не найдено"));
    }

    @Test
    void shouldGetWorksByStudent() throws Exception {
        Work work = new Work();
        work.setId(1L);
        work.setStudentName("Иван");
        work.setOriginalFileName("test.txt");

        when(workService.getWorksByStudentName("Иван")).thenReturn(List.of(work));

        mockMvc.perform(get("/api/works/by-student").param("name", "Иван"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].studentName").value("Иван"));
    }
}
