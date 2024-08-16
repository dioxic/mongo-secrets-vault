package uk.dioxic.mongo.secrets;

import com.mongodb.ClientEncryptionSettings;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.vault.DataKeyOptions;
import com.mongodb.client.model.vault.EncryptOptions;
import com.mongodb.client.vault.ClientEncryption;
import com.mongodb.client.vault.ClientEncryptions;
import org.bson.BsonBinary;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.types.Binary;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

import static com.mongodb.client.model.Indexes.ascending;

public class SecretService implements ISecretService {

    private static final String DATA_KEY_ALT_NAME = "dek";
    private static final String VAULT_DB = "csfle";
    private static final String METADATA_COLLECTION = "metadata";
    private static final String VAULT_COLLECTION_SUFFIX = "_vault";
    private static final String SECRETS_COLLECTION_PREFIX = "secrets_";
    private final MongoClient client;
    private final MongoClientSettings mongoClientSettings;
    private final Map<Color, ClientEncryption> vaultMap;
    private final Map<Color, byte[]> masterKeyMap;
    private final MongoDatabase database;

    public SecretService(String connectionString, String blueKey, String greenKey) {
        // connect to MongoDB
        var cs = new ConnectionString(connectionString);
        mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(cs)
                .build();
        client = MongoClients.create(mongoClientSettings);
        this.database = client.getDatabase(Objects.requireNonNullElse(cs.getDatabase(), "secrets"));

        // populate the master key map
        this.masterKeyMap = new HashMap<>() {{
            put(Color.GREEN, transformKey(greenKey));
            put(Color.BLUE, transformKey(blueKey));
        }};

        // populate the vault map
        this.vaultMap = new HashMap<>();
        for (Color color : masterKeyMap.keySet()) {
            vaultMap.put(color, createClientEncryption(color));
        }

    }

    /**
     * Transforms a string into a 96 byte array
     *
     * @return byte array key
     */
    public static byte[] transformKey(String key) {
        try {
            var md = MessageDigest.getInstance("MD5");
            md.update(key.getBytes());
            return Arrays.copyOfRange(md.digest(), 0, 96);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void write(String secretId, String secret) {
        write(secretId, secret, "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic");
    }

    @Override
    public void write(String secretId, String secret, String algorithm) {
        var options = new EncryptOptions(algorithm);
        for (Color color : Color.values()) {
            write(secretId, secret, color, options);
        }
    }

    public void write(String secretId, String secret, Color color, EncryptOptions encryptOptions) {
        assert vaultMap.containsKey(color) : "No vault configured for color " + color.name();

//        Document document = new Document();
//        document.put("_id", secretId);
//        document.put("secret", encrypt(secret, color, encryptOptions));

        getSecretsCollection(color).updateOne(
                Filters.eq(secretId),
                Updates.set("secret", encrypt(secret, color, encryptOptions)),
                new UpdateOptions().upsert(true)
        );
    }

    private Document encrypt(Document document, Color color, EncryptOptions encryptOptions) {
        document.computeIfPresent("secret", (key, val) -> encrypt(val.toString(), color, encryptOptions));
        return document;
    }

    private BsonBinary encrypt(String plainText, Color color, EncryptOptions encryptOptions) {
        return vaultMap.get(color).encrypt(new BsonString(plainText), encryptOptions.keyAltName(DATA_KEY_ALT_NAME));
    }

    private MongoCollection<Document> getSecretsCollection(Color color) {
        return database.getCollection(SECRETS_COLLECTION_PREFIX + color.name().toLowerCase());
    }

    @Override
    public String read(String secretId) {
        return read(secretId, getActive());
    }

    public String read(String secretId, Color color) {
        assert vaultMap.containsKey(color) : "No vault configured for color " + color;

        var document = getSecretsCollection(color)
                .find(Filters.eq("_id", secretId))
                .first();

        assert document != null : "No secret found for id: " + secretId;

        return decrypt(document, color).getString("secret");
    }

    private Document decrypt(Document document, Color color) {
        document.computeIfPresent("secret", (key, val) -> decrypt((Binary) val, color));
        return document;
    }

    private String decrypt(Binary binary, Color color) {
        assert vaultMap.containsKey(color) : "No vault configured for color " + color.name();
        BsonBinary cipherText = new BsonBinary(binary.getType(), binary.getData());
        return vaultMap.get(color).decrypt(cipherText).asString().getValue();
    }

    @Override
    public Color getActive() {
        var meta = client
                .getDatabase(VAULT_DB)
                .getCollection(METADATA_COLLECTION)
                .find(Filters.eq("SecretService"))
                .first();
        assert meta != null : "No metadata found with _id=SecretService";
        assert meta.containsKey("active") : "No metadata found for active color";
        return Color.valueOf(meta.getString("active"));
    }

    @Override
    public void activate(Color color) {
        client
                .getDatabase(VAULT_DB)
                .getCollection(METADATA_COLLECTION)
                .updateOne(
                        Filters.eq("SecretService"),
                        Updates.set("active", color.name()),
                        new UpdateOptions().upsert(true)
                );
    }

    @Override
    public long rotate(byte[] masterKey, String algorithm) {
        var encryptOptions = new EncryptOptions(algorithm);
        var activeColor = getActive();
        var inactiveColor = activeColor.flip();
        var activeCollection = getSecretsCollection(activeColor);
        var inactiveCollection = getSecretsCollection(activeColor.flip());
        long count = 0;

        // drop the inactive secrets
        inactiveCollection.drop();

        // update the master key for the inactive color
        masterKeyMap.put(inactiveColor, masterKey);

        // re-create the vault for the inactive color
        vaultMap.put(inactiveColor, createClientEncryption(inactiveColor));

        // wipe and recreate the inactive key vault
        initializeKeyVault(inactiveColor);

        // read the active secrets, re-encrypt with the new key and write to the inactive color secrets collection
        for (Document encDoc : activeCollection.find()) {
            var decryptedDoc = decrypt(encDoc, activeColor);
            var rotatedEncDoc = encrypt(decryptedDoc, inactiveColor, encryptOptions);
            inactiveCollection.insertOne(rotatedEncDoc);
            count++;
        }

        return count;
    }

    @Override
    public void info() {

    }

    public byte[] generateMasterKey() {
        byte[] cmk = new byte[96];
        new SecureRandom().nextBytes(cmk);
        return cmk;
    }

    @Override
    public void initialize() {
        for (Color color : Color.values()) {
            initializeKeyVault(color);
            getSecretsCollection(color).drop();
        }
    }

    private ClientEncryption createClientEncryption(Color color) {
        assert masterKeyMap.containsKey(color) : "No master key found for color " + color.name();
        ClientEncryptionSettings clientEncryptionSettings = ClientEncryptionSettings.builder()
                .keyVaultMongoClientSettings(mongoClientSettings)
                .keyVaultNamespace(getKeyVaultNamespace(color))
                .kmsProviders(getKmsProviders(masterKeyMap.get(color)))
                .build();
        return ClientEncryptions.create(clientEncryptionSettings);
    }

    private void initializeKeyVault(Color color) {
        MongoCollection<Document> keyVaultCollection = client
                .getDatabase(VAULT_DB)
                .getCollection(color.name().toLowerCase() + VAULT_COLLECTION_SUFFIX);

        keyVaultCollection.drop();
        IndexOptions indexOpts = new IndexOptions()
                .partialFilterExpression(new Document("keyAltNames", new Document("$exists", true)))
                .unique(true);

        keyVaultCollection.createIndex(ascending("keyAltNames"), indexOpts);

        createDataKey(vaultMap.get(color));
    }

    private void createDataKey(ClientEncryption clientEncryption) {
        DataKeyOptions options = new DataKeyOptions().keyAltNames(List.of(DATA_KEY_ALT_NAME));
        clientEncryption.createDataKey("local", options);
    }

    private static Map<String, Map<String, Object>> getKmsProviders(byte[] masterKey) {
        return new HashMap<>() {{
            put("local", new HashMap<>() {{
                put("key", masterKey);
            }});
        }};
    }

    private static String getKeyVaultNamespace(Color color) {
        return VAULT_DB + "." + color.name().toLowerCase() + VAULT_COLLECTION_SUFFIX;
    }
}
