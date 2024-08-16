package uk.dioxic.mongo.secrets.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import uk.dioxic.mongo.secrets.Color;
import uk.dioxic.mongo.secrets.SecretService;

import java.util.concurrent.Callable;

@Command(name = "activate", description = "Activate a Color")
public class ActivateCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "The colour to activate (${COMPLETION-CANDIDATES})")
    private Color color;

    @Option(names = {"--uri"}, description = "MongoDB connection string (default: ${DEFAULT-VALUE})", defaultValue = "mongodb://localhost:27017")
    private String uri;

    @Override
    public Integer call() {
        var secretService = new SecretService(uri, "passwordBLUE", "passwordGREEN");
        secretService.activate(color);
        System.out.println(color + " activated");
        return 0;
    }
}
