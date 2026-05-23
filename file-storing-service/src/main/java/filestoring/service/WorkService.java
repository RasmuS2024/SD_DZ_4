package filestoring.service;

import filestoring.domain.Work;
import filestoring.dto.FileData;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

public interface WorkService {

    Work saveWork(String studentName, MultipartFile file) throws IOException;

    FileData getWorkFile(Long workId) throws Exception;

    List<Work> getWorksByStudentName(String name);

    Work getWorkById(Long id);

}