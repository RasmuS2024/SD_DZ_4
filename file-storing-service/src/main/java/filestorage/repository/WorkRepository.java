package filestorage.repository;

import filestorage.domain.Work;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkRepository extends JpaRepository<Work, Long> {
    List<Work> findByStudentName(String name);
}
