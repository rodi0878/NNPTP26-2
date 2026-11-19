package cz.upce.fei.nnptp.taskpulse;

import cz.upce.fei.nnptp.taskpulse.domain.Project;
import cz.upce.fei.nnptp.taskpulse.domain.Task;
import cz.upce.fei.nnptp.taskpulse.domain.Worklog;
import cz.upce.fei.nnptp.taskpulse.storage.StorageException;
import cz.upce.fei.nnptp.taskpulse.storage.csv.CsvStorageManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskPulseAppTest {

    @TempDir
    Path dataDir;

    @Test
    void interactiveProjectIsOnDiskWhenCreationIsReported() {
        var confirmedProjects = new ArrayList<String>();
        var input = new ByteArrayInputStream("project add Exams\nexit\n".getBytes(StandardCharsets.UTF_8));
        try (var output = new PrintStream(OutputStream.nullOutputStream()) {
            @Override
            public void println(String line) {
                if (line.startsWith("Project created:")) {
                    confirmedProjects.add(onlyProject().getName());
                }
            }
        }) {
            TaskPulseApp.run(new String[0], dataDir, input, output, System.err);
        }
        assertThat(confirmedProjects).containsExactly("Exams");
    }

    @Test
    void listingProjectsDoesNotCreateWorkspaceFiles() {
        Path workspace = dataDir.resolve("empty");
        TaskPulseApp.run(new String[]{"project", "list"}, workspace);
        assertThat(workspace).doesNotExist();
    }

    @Test
    void failedSaveDoesNotConfirmProjectCreation() throws Exception {
        Path blocked = Files.createFile(dataDir.resolve("file"));
        var messages = new ByteArrayOutputStream();
        try (var output = new PrintStream(messages)) {
            String[] args = {"project", "add", "Exams"};
            assertThatThrownBy(() -> TaskPulseApp.run(args, blocked, InputStream.nullInputStream(), output, System.err))
                    .isInstanceOf(StorageException.class).hasMessageContaining("Failed to save workspace");
        }
        assertThat(messages.size()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Programming course", "Fix \"Save as\" dialog", "C:\\Program Files\\new"})
    void oneShotArgumentWithSpecialCharactersStaysOneValue(String name) {
        TaskPulseApp.run(new String[]{"project", "add", name}, dataDir);

        Project project = onlyProject();
        assertThat(project.getName()).isEqualTo(name);
        assertThat(project.getDescription()).isEmpty();
    }

    @Test
    void oneShotKeepsSeparateArgumentsWithSpacesApart() {
        TaskPulseApp.run(new String[]{"project", "add", "Programming course", "Semester work"}, dataDir);

        Project project = onlyProject();
        assertThat(project.getName()).isEqualTo("Programming course");
        assertThat(project.getDescription()).isEqualTo("Semester work");
    }

    @Test
    void oneShotKeepsTitleAndFlagValuesWithSpacesAcrossCommands() {
        TaskPulseApp.run(new String[]{"project", "add", "Course"}, dataDir);
        String projectId = onlyProject().getId().toString();

        TaskPulseApp.run(new String[]{"task", "add", projectId, "Implement CSV escaping"}, dataDir);
        Task task = onlyTask();
        assertThat(task.getTitle()).isEqualTo("Implement CSV escaping");

        String taskId = task.getId().toString();
        TaskPulseApp.run(new String[]{"task", "transition", taskId, "IN_PROGRESS"}, dataDir);
        TaskPulseApp.run(new String[]{"worklog", "log", taskId, "45", "--author", "Student Developer", "--desc", "First pass"}, dataDir);

        Worklog worklog = onlyTask().getWorklogs().get(0);
        assertThat(worklog.author()).isEqualTo("Student Developer");
        assertThat(worklog.description()).isEqualTo("First pass");
    }

    private Project onlyProject() {
        var projects = new CsvStorageManager(dataDir).load().projectRepository().findAll();
        assertThat(projects).hasSize(1);
        return projects.get(0);
    }

    private Task onlyTask() {
        var tasks = new CsvStorageManager(dataDir).load().taskRepository().findAll();
        assertThat(tasks).hasSize(1);
        return tasks.get(0);
    }
}
