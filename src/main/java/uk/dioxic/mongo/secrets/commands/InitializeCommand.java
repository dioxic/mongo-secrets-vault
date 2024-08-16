package uk.dioxic.mongo.secrets.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import uk.dioxic.mongo.secrets.Color;
import uk.dioxic.mongo.secrets.SecretService;

import java.util.concurrent.Callable;

@Command(name = "init", description = "Initializes vaults - drops existing vaults and secrets")
public class InitializeCommand implements Callable<Integer> {

    @Option(names = {"--uri"}, description = "MongoDB connection string (default: ${DEFAULT-VALUE})", defaultValue = "mongodb://localhost:27017")
    private String uri;

    @Option(names = {"--blue-key"}, description = "The blue master key (default: ${DEFAULT-VALUE})", defaultValue = "passwordBLUE")
    private String blueKey;

    @Option(names = {"--green-key"}, description = "The green master key (default: ${DEFAULT-VALUE})", defaultValue = "passwordGREEN")
    private String greenKey;

    @Option(names = {"--active-vault"}, description = "The colour to activate (${COMPLETION-CANDIDATES})", defaultValue = "GREEN")
    private Color activeVault;

    @Override
    public Integer call() {
        var secretService = new SecretService(uri, blueKey, greenKey);
        System.out.println("Initializing vaults...");
        secretService.initialize(activeVault);
        System.out.println("Vaults initialized!");
        return 0;
    }
}