package uk.dioxic.mongo.secrets.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import uk.dioxic.mongo.secrets.SecretService;

import java.util.concurrent.Callable;

@Command(name = "info", description = "Shows information about the vault")
public class InfoCommand implements Callable<Integer> {

    @Option(names = {"--uri"}, description = "MongoDB connection string", defaultValue = "mongodb://localhost:27017")
    private String uri;

    @Option(names = {"--blue-key"}, description = "The blue master key (default: ${DEFAULT-VALUE})", defaultValue = "passwordBLUE")
    private String blueKey;

    @Option(names = {"--green-key"}, description = "The green master key (default: ${DEFAULT-VALUE})", defaultValue = "passwordGREEN")
    private String greenKey;

    @Override
    public Integer call() {
        var secretService = new SecretService(uri, blueKey, greenKey);
        var active = secretService.getActive();
        System.out.println("Active Color: " + active);
        return 0;
    }
}