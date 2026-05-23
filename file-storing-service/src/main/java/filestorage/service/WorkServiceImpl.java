package filestorage.service;

import filestorage.domain.Work;
import filestorage.dto.FileData;
import filestorage.repository.WorkRepository;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

@Service
public class WorkServiceImpl implements WorkService {

    private static final Logger log = LoggerFactory.getLogger(WorkServiceImpl.class);

    private final WorkRepository workRepository;
    private final MinioClient minioClient;
    private boolean bucketReady = false;

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
        return workRepository.save(work);
    }

    @Override
    public FileData getWorkFile(Long workId) throws Exception {
        if (!bucketReady) {
            throw new Exception("Файловое хранилище недоступно");
        }

        Work work = workRepository.findById(workId)
                .orElseThrow(() -> new RuntimeException("Работа не найдена"));

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
}
