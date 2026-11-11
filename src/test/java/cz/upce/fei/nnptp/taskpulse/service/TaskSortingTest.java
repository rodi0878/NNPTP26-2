package cz.upce.fei.nnptp.taskpulse.service;

import cz.upce.fei.nnptp.taskpulse.domain.Priority;
import cz.upce.fei.nnptp.taskpulse.domain.Project;
import cz.upce.fei.nnptp.taskpulse.domain.Task;
import cz.upce.fei.nnptp.taskpulse.storage.InMemoryProjectRepository;
import cz.upce.fei.nnptp.taskpulse.storage.InMemoryTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TaskSortingTest {

    private InMemoryProjectRepository projectRepository;
    private InMemoryTaskRepository taskRepository;
    private ProjectService projectService;
    private TaskService taskService;
    private Project project;

    @BeforeEach
    void setUp() {
        projectRepository = new InMemoryProjectRepository();
        taskRepository = new InMemoryTaskRepository();
        projectService = new ProjectService(projectRepository, taskRepository);
        taskService = new TaskService(taskRepository, projectRepository);
        project = projectService.createProject("Sorting Test", "");
    }

    @Test
    void sortByTitleAscendingCaseInsensitive() {
        Task t1 = taskService.createTask(project.getId(), "banana", "", Priority.LOW, null, null);
        Task t2 = taskService.createTask(project.getId(), "Apple", "", Priority.LOW, null, null);
        Task t3 = taskService.createTask(project.getId(), "cherry", "", Priority.LOW, null, null);

        TaskQuery query = TaskQuery.builder().sortOrder(TaskSortOrder.TITLE_ASC).build();
        List<Task> sorted = taskService.findTasks(query);

        assertThat(sorted).containsExactly(t2, t1, t3);
    }

    @Test
    void sortByPriorityDescending() {
        Task t1 = taskService.createTask(project.getId(), "T1", "", Priority.LOW, null, null);
        Task t2 = taskService.createTask(project.getId(), "T2", "", Priority.CRITICAL, null, null);
        Task t3 = taskService.createTask(project.getId(), "T3", "", Priority.MEDIUM, null, null);
        Task t4 = taskService.createTask(project.getId(), "T4", "", Priority.HIGH, null, null);

        TaskQuery query = TaskQuery.builder().sortOrder(TaskSortOrder.PRIORITY_DESC).build();
        List<Task> sorted = taskService.findTasks(query);

        assertThat(sorted).containsExactly(t2, t4, t3, t1);
    }

    @Test
    void sortByDueDateAscendingWithNullsLast() {
        LocalDate d1 = LocalDate.of(2026, 11, 1);
        LocalDate d2 = LocalDate.of(2026, 11, 10);

        Task noDate = taskService.createTask(project.getId(), "No Date", "", Priority.LOW, null, null);
        Task lateDate = taskService.createTask(project.getId(), "Late", "", Priority.LOW, null, d2);
        Task earlyDate = taskService.createTask(project.getId(), "Early", "", Priority.LOW, null, d1);

        TaskQuery query = TaskQuery.builder().sortOrder(TaskSortOrder.DUE_DATE_ASC).build();
        List<Task> sorted = taskService.findTasks(query);

        assertThat(sorted).containsExactly(earlyDate, lateDate, noDate);
    }

    @Test
    void sortByPriorityDescendingThenDueDateAscending() {
        LocalDate early = LocalDate.of(2026, 11, 1);
        LocalDate late = LocalDate.of(2026, 11, 10);

        Task critLate = taskService.createTask(project.getId(), "Crit Late", "", Priority.CRITICAL, null, late);
        Task critEarly = taskService.createTask(project.getId(), "Crit Early", "", Priority.CRITICAL, null, early);
        Task highEarly = taskService.createTask(project.getId(), "High Early", "", Priority.HIGH, null, early);

        TaskQuery query = TaskQuery.builder().sortOrder(TaskSortOrder.PRIORITY_DESC_THEN_DUE_DATE_ASC).build();
        List<Task> sorted = taskService.findTasks(query);

        assertThat(sorted).containsExactly(critEarly, critLate, highEarly);
    }
    
        @Test
    void equalPrioritiesAreSortedById() {
        assertTiesAreSortedById(TaskSortOrder.PRIORITY_DESC, null);
    }

    @Test
    void equalDueDatesAreSortedById() {
        assertTiesAreSortedById(
                TaskSortOrder.DUE_DATE_ASC, LocalDate.of(2026, 11, 10));
    }

    @Test
    void equalPrioritiesAndDueDatesAreSortedById() {
        assertTiesAreSortedById(
                TaskSortOrder.PRIORITY_DESC_THEN_DUE_DATE_ASC,
                LocalDate.of(2026, 11, 10));
    }

    @Test
    void missingDueDatesAreSortedById() {
        assertTiesAreSortedById(TaskSortOrder.DUE_DATE_ASC, null);
    }

    @Test
    void titleSortingStillTakesPrecedenceOverId() {
        Task zebra = saveTask("00000000-0000-0000-0000-000000000001",
                "Zebra", null);
        Task alpha = saveTask("00000000-0000-0000-0000-000000000002",
                "Alpha", null);

        TaskQuery query = TaskQuery.builder()
                .sortOrder(TaskSortOrder.TITLE_ASC)
                .build();

        assertThat(taskService.findTasks(query)).containsExactly(alpha, zebra);
    }

    private void assertTiesAreSortedById(TaskSortOrder order, LocalDate dueDate) {
        Task alpha = saveTask("00000000-0000-0000-0000-000000000002",
                "Alpha", dueDate);
        Task zebra = saveTask("00000000-0000-0000-0000-000000000001",
                "Zebra", dueDate);

        TaskQuery query = TaskQuery.builder()
                .sortOrder(order)
                .build();

        assertThat(taskService.findTasks(query)).containsExactly(zebra, alpha);
    }

    private Task saveTask(String id, String title, LocalDate dueDate) {
        Task task = new Task(UUID.fromString(id), project.getId(), title);
        task.setPriority(Priority.HIGH);
        task.setDueDate(dueDate);
        return taskRepository.save(task);
    }
}
