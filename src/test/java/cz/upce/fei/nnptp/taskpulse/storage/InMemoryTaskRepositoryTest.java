package cz.upce.fei.nnptp.taskpulse.storage;

import cz.upce.fei.nnptp.taskpulse.domain.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryTaskRepositoryTest {

    private InMemoryTaskRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTaskRepository();
    }

    @Test
    void deleteExistingTaskReturnsTrueAndRemovesTask() {
        Task task = new Task(UUID.randomUUID(), "Test Task");
        repository.save(task);

        boolean deleted = repository.deleteById(task.getId());

        assertThat(deleted).isTrue();
        assertThat(repository.findById(task.getId())).isEmpty();
    }

    @Test
    void deleteNonExistentTaskReturnsFalse() {
        UUID nonExistentId = UUID.randomUUID();

        boolean deleted = repository.deleteById(nonExistentId);

        assertThat(deleted).isFalse();
    }
}