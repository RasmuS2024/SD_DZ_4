package filestorage.dto;

import java.io.InputStream;

public record FileData(
        InputStream inputStream,
        String fileName,
        String contentType,
        long contentLength
) {}
