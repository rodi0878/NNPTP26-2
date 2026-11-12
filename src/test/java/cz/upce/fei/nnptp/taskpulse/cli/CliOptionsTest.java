package cz.upce.fei.nnptp.taskpulse.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CliOptionsTest {

    static Stream<String> valueOptions() {
        return Stream.of("--project", "--state", "--priority", "--label", "--search",
                "--sort", "--from", "--to", "--author", "--desc");
    }

    static Stream<Arguments> optionsFollowedBySwitch() {
        return valueOptions().flatMap(option -> Stream.of("--desc", "--overdue", "--unknown")
                .map(next -> Arguments.of(option, next)));
    }

    @ParameterizedTest
    @MethodSource("optionsFollowedBySwitch")
    void rejectsAnotherSwitchAsValue(String option, String next) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> CliOptions.parse(List.of("list", option, next, "text"), 1))
                .withMessage("Missing value for option " + option);
    }

    @ParameterizedTest
    @MethodSource("valueOptions")
    void acceptsValueBeforeAnotherSwitch(String option) {
        CliOptions options = CliOptions.parse(List.of("list", option, "some value", "--overdue"), 1);

        assertThat(options.get(option)).isEqualTo("some value");
        assertThat(options.has("--overdue")).isTrue();
    }

    @Test
    void acceptsFlagsWithoutValuesBeforeAnotherSwitch() {
        CliOptions options = CliOptions.parse(List.of("--overdue", "--exceeded", "--state", "TODO"), 0);

        assertThat(options.has("--overdue")).isTrue();
        assertThat(options.get("--exceeded")).isEmpty();
        assertThat(options.get("--state")).isEqualTo("TODO");
    }

    @Test
    void acceptsExceededThresholdBeforeAnotherSwitch() {
        CliOptions options = CliOptions.parse(List.of("--exceeded", "30", "--state", "TODO"), 0);

        assertThat(options.get("--exceeded")).isEqualTo("30");
        assertThat(options.get("--state")).isEqualTo("TODO");
    }
}
