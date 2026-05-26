package filestoring.service;

import filestoring.domain.Work;
import filestoring.dto.FileData;
import filestoring.exception.FileStoringException;
import filestoring.repository.WorkRepository;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import okhttp3.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class WorkServiceImplTest {

    @Mock
    private WorkRepository workRepository;

    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private WorkServiceImpl workService;

    private Work work;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(workService, "bucketReady", true);
        ReflectionTestUtils.setField(workService, "bucketName", "test-bucket");

        work = new Work();
        work.setId(1L);
        work.setStudentName("Иван");
        work.setOriginalFileName("test.txt");
        work.setS3Key("uuid_test.txt");
    }

    @Test
    @DisplayName("Сохранение работы: загрузка в MinIO и сохранение в БД")
    void saveWork_shouldSaveAndUpload_whenValidInput() throws Exception {
        MultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "content".getBytes());
        when(workRepository.save(any(Work.class))).thenReturn(work);

        Work result = workService.saveWork("Иван", file);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStudentName()).isEqualTo("Иван");
        assertThat(result.getOriginalFileName()).isEqualTo("test.txt");
        verify(minioClient).putObject(any(PutObjectArgs.class));
        verify(workRepository).save(any(Work.class));
    }

    @Test
    @DisplayName("Сохранение работы: исключение при недоступном bucket")
    void saveWork_shouldThrowException_whenBucketNotReady() {
        ReflectionTestUtils.setField(workService, "bucketReady", false);
        MultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "content".getBytes());

        assertThatThrownBy(() -> workService.saveWork("Иван", file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Файловое хранилище недоступно");
        verifyNoInteractions(workRepository, minioClient);
    }

    @Test
    @DisplayName("Получение файла: успешный сценарий")
    void getWorkFile_shouldReturnFileData_whenWorkExists() throws Exception {
        when(workRepository.findById(1L)).thenReturn(Optional.of(work));

        Headers headers = mock(Headers.class);
        when(headers.get("Content-Type")).thenReturn("text/plain");
        GetObjectResponse getResponse = mock(GetObjectResponse.class);
        when(getResponse.headers()).thenReturn(headers);
        StatObjectResponse statResponse = mock(StatObjectResponse.class);
        when(statResponse.size()).thenReturn(4L);

        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(getResponse);
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(statResponse);

        FileData result = workService.getWorkFile(1L);

        assertThat(result).isNotNull();
        assertThat(result.fileName()).isEqualTo("test.txt");
        assertThat(result.contentLength()).isEqualTo(4L);
        verify(workRepository).findById(1L);
        verify(minioClient).getObject(any(GetObjectArgs.class));
        verify(minioClient).statObject(any(StatObjectArgs.class));
    }

    @Test
    @DisplayName("Получение файла: исключение при отсутствии работы")
    void getWorkFile_shouldThrowException_whenWorkNotFound() {
        when(workRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workService.getWorkFile(99L))
                .isInstanceOf(FileStoringException.class)
                .hasMessageContaining("Работа не найдена");
    }

    @Test
    @DisplayName("Получение работы по ID: успешный сценарий")
    void getWorkById_shouldReturnWork_whenExists() {
        when(workRepository.findById(1L)).thenReturn(Optional.of(work));

        Work result = workService.getWorkById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStudentName()).isEqualTo("Иван");
    }

    @Test
    @DisplayName("Получение работы по ID: исключение при отсутствии")
    void getWorkById_shouldThrowException_whenNotFound() {
        when(workRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workService.getWorkById(99L))
                .isInstanceOf(FileStoringException.class)
                .hasMessageContaining("Работа не найдена");
    }

    @Test
    @DisplayName("Поиск работ по имени студента")
    void getWorksByStudentName_shouldReturnList() {
        when(workRepository.findByStudentName("Иван")).thenReturn(List.of(work));

        List<Work> result = workService.getWorksByStudentName("Иван");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStudentName()).isEqualTo("Иван");
    }
}
