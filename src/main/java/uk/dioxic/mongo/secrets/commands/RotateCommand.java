package uk.dioxic.mongo.secrets.commands;

import com.mongodb.MongoException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import uk.dioxic.mongo.secrets.SecretService;

import java.util.concurrent.Callable;

@Command(name = "rotate", description = "Rotate the master key for the inactive color")
public class RotateCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "The new master key")
    private String key;

    @Option(names = {"--uri"}, description = "MongoDB connection string (default: ${DEFAULT-VALUE})", defaultValue = "mongodb://localhost:27017")
    private String uri;

    @Option(names = {"--blue-key"}, description = "The blue master key (default: ${DEFAULT-VALUE})", defaultValue = "passwordBLUE")
    private String blueKey;

    @Option(names = {"--green-key"}, description = "The green master key (default: ${DEFAULT-VALUE})", defaultValue = "passwordGREEN")
    private String greenKey;

    @Option(names = { "--algorithm"}, description = "The encryption algorithm (default: ${DEFAULT-VALUE})", defaultValue = "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic")
    private String algorithm;

    @Override
    public Integer call() {
        var secretService = new SecretService(uri, blueKey, greenKey);
        try {
            var inactive = secretService.getActive().flip();
            System.out.println("Rotating secrets in " + inactive + " vault...");
            var count = secretService.rotate(SecretService.transformKey(key), algorithm);
            System.out.println(count + " secrets rotated for " + inactive);
        } catch (MongoException e) {
            if (e.getMessage().equals("HMAC validation failure")) {
                System.err.println("Rotation failed - are you using the correct key?");
                return 1;
            }
        }
        return 0;
    }
}