package filestorage.service;

import filestorage.domain.Work;
import filestorage.repository.WorkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkServiceImpl implements WorkService {
    private final WorkRepository workRepository;

    @Override
    public Work addWork() {
        Work work = new Work();
        return workRepository.save(work);
    }

    @Override
    public Work getWorkByName(String name) {

    }

}
