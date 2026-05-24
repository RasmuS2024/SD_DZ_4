package filestoring.service;

import filestoring.domain.Work;
import filestoring.dto.FileData;
import filestoring.exception.FileStoringException;
import filestoring.repository.WorkRepository;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.MinioException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class WorkServiceImpl implements WorkService {
    private final WorkRepository workRepository;
    private final MinioClient minioClient;
    private boolean bucketReady = false;

    @Value("${app.analysis-service.url}")
    private String analysisServiceUrl;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public WorkServiceImpl(WorkRepository workRepository, MinioClient minioClient) {
        this.workRepository = workRepository;
        this.minioClient = minioClient;
    }

    @PostConstruct
    public void init() {
        createBucketIfNotExists();
    }

    private void createBucketIfNotExists() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
            bucketReady = true;
            log.info("Хранилище '{}' готово к работе", bucketName);
        } catch (Exception e) {
            log.error("Не удалось создать хранилище '{}': {}", bucketName, e.getMessage());
            log.warn("Сервис запущен, но файловое хранилище недоступно");
        }
    }

    @Override
    public Work saveWork(String studentName, MultipartFile file) throws IOException {
        if (!bucketReady) {
            throw new IOException("Файловое хранилище недоступно");
        }

        String s3Key = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(s3Key)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new IOException("Ошибка загрузки файла в хранилище MinIO", e);
        }

        Work work = new Work();
        work.setStudentName(studentName);
        work.setOriginalFileName(file.getOriginalFilename());
        work.setS3Key(s3Key);
        Work savedWork = workRepository.save(work);

        triggerAnalysis(savedWork.getId());

        return savedWork;
    }

    private void triggerAnalysis(Long workId) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = analysisServiceUrl + "/api/analysis/works/" + workId + "/report";
            restTemplate.getForEntity(url, String.class);
            log.info("Анализ для работы {} запущен", workId);
        } catch (Exception e) {
            log.error("Ошибка при запуске анализа для работы {}: {}", workId, e.getMessage());
        }
    }

    @Override
    public FileData getWorkFile(Long workId) throws Exception {
        if (!bucketReady) {
            throw new FileStoringException("Файловое хранилище недоступно");
        }

        Work work = workRepository.findById(workId)
                .orElseThrow(() -> new FileStoringException("Работа не найдена"));

        GetObjectResponse response = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(work.getS3Key())
                        .build()
        );

        StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(work.getS3Key())
                        .build()
        );

        return new FileData(
                response,
                work.getOriginalFileName(),
                response.headers().get("Content-Type"),
                stat.size()
        );
    }

    @Override
    public List<Work> getWorksByStudentName(String name) {

        return workRepository.findByStudentName(name);
    }

    @Override
    public Work getWorkById(Long id) {

        return workRepository.findById(id)
                .orElseThrow(() -> new FileStoringException("Работа не найдена"));
    }
}
