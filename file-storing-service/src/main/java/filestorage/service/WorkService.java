package filestorage.service;

import filestorage.domain.Work;

public interface WorkService {
    Work addWork();
    Work getWorkByName(String name);
}
