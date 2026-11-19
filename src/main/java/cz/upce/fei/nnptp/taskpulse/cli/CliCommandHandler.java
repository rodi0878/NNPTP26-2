package cz.upce.fei.nnptp.taskpulse.cli;

import java.util.List;

interface CliCommandHandler {
    /** Returns the confirmation to print after saving a change, or null if there is nothing to save. */
    String handle(List<String> args);
}
