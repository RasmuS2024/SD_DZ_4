package filestoring.controller;

import filestoring.domain.Work;
import filestoring.dto.FileData;
import filestoring.service.WorkService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/works")
@RequiredArgsConstructor
public class WorkController {

    private final WorkService workService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Work> uploadWork(
            @RequestParam("studentName") @NotBlank String studentName,
            @RequestParam("file") MultipartFile file) throws IOException {

        Work savedWork = workService.saveWork(studentName, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedWork);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable Long id) throws Exception {
        FileData fileData = workService.getWorkFile(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(fileData.fileName()).build());
        headers.setContentType(MediaType.parseMediaType(fileData.contentType()));

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(fileData.contentLength())
                .body(new InputStreamResource(fileData.inputStream()));
    }

    @GetMapping("/by-student")
    public ResponseEntity<List<Work>> getWorksByStudent(@RequestParam String name) {
        List<Work> works = workService.getWorksByStudentName(name);
        return ResponseEntity.ok(works);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Work> getWork(@PathVariable Long id) {
        Work work = workService.getWorkById(id);
        return ResponseEntity.ok(work);
    }
}