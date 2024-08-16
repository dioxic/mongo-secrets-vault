package uk.dioxic.mongo.secrets.commands;

import com.mongodb.MongoException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import uk.dioxic.mongo.secrets.SecretService;

import java.util.concurrent.Callable;

@Command(name = "read", description = "Read a secret")
public class ReadCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "The ID of the secret to read")
    private String secretId;

    @Option(names = {"--uri"}, description = "MongoDB connection string (default: ${DEFAULT-VALUE})", defaultValue = "mongodb://localhost:27017")
    private String uri;

    @Option(names = {"--blue-key"}, description = "The blue master key (default: ${DEFAULT-VALUE})", defaultValue = "passwordBLUE")
    private String blueKey;

    @Option(names = {"--green-key"}, description = "The green master key (default: ${DEFAULT-VALUE})", defaultValue = "passwordGREEN")
    private String greenKey;

    @Override
    public Integer call() throws Exception {
        var secretService = new SecretService(uri, blueKey, greenKey);
        try {
            System.out.println("Reading secret " + secretId + " from " + secretService.getActive() + "...");
            System.out.println(secretId + "=" + secretService.read(secretId));
        } catch (MongoException e) {
            if (e.getMessage().equals("HMAC validation failure")) {
                System.err.println("Read failed - are you using the correct key?");
                return 1;
            }
        }
        return 0;
    }
}