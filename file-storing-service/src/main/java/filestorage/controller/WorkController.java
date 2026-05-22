package filestorage.controller;

import filestorage.domain.Work;
import filestorage.service.WorkServiceImpl;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WorkController {
    private final WorkServiceImpl workService;

    @PostMapping("/add-work")
    public ResponseEntity<Void> addWork() {
        workService.addWork();
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/name/{name}")
    @ApiResponse(responseCode = "200", description = "Работа успешно получена")
    @ApiResponse(responseCode = "404", description = "Работа не найдена")
    public ResponseEntity<Work> getWorkByName(@PathVariable String name) {
        Work work = workService.getWorkByName(name);
        return ResponseEntity.ok(work);
    }
}
