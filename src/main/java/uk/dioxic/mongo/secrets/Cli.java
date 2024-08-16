package uk.dioxic.mongo.secrets;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import uk.dioxic.mongo.secrets.commands.*;

@Command(name = "secrets", mixinStandardHelpOptions = true, version = "1.0", description = "MongoDB secrets CLI tool",
        subcommands = {
                ActivateCommand.class,
                InitializeCommand.class,
                InfoCommand.class,
                ReadCommand.class,
                RotateCommand.class,
                WriteCommand.class
        })
class Cli {

    public static void main(String... args) {
        int exitCode = new CommandLine(new Cli()).execute(args);
        System.exit(exitCode);
    }
}
