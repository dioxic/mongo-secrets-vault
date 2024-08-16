package uk.dioxic.mongo.secrets.commands;

import com.mongodb.MongoException;
import org.bson.types.ObjectId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import uk.dioxic.mongo.secrets.SecretService;

import java.util.concurrent.Callable;

@Command(name = "write", description = "Write a secret")
public class WriteCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "The secret contents")
    private String secret;

    @Option(names = {"--secret-id"}, description = "The ID of the secret to write")
    private String secretId;

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
        if (secretId == null) {
            secretId = ObjectId.get().toHexString();
        }

        var secretService = new SecretService(uri, blueKey, greenKey);

        try {
            System.out.println("Writing secret to BLUE & GREEN vaults...");
            secretService.write(secretId, secret, algorithm);
            System.out.println("Secret written (id: " + secretId + ")");
        } catch (MongoException e) {
            if (e.getMessage().equals("HMAC validation failure")) {
                System.err.println("Write failed - are you using the correct keys?");
                return 1;
            }
        }
        return 0;
    }
}