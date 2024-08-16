package uk.dioxic.mongo.secrets;

public interface ISecretService {

    /**
     * Writes a new secret to all Colors
     * @param secretId secret identifier
     * @param secret secret data
     */
    void write(String secretId, String secret);

    /**
     * Writes a new secret to all Colors
     * @param secretId secret identifier
     * @param secret secret data
     * @param algorithm encryption algorithm
     */
    void write(String secretId, String secret, String algorithm);

    /**
     * Reads a secret for the active color
     * @param secretId secret identifier
     * @return secret data
     */
    String read(String secretId);

    /**
     * Re-encrypts the data of the inactive color by reading data from the active color
     * and encrypting it with a different key into the inactive color
     * @param masterKey the master key for rotation
     * @return the number of secrets rotated
     */
    long rotate(byte[] masterKey, String algorithm);

    /**
     * Sets a Color as the active color.
     * Other Colors will be made inactive.
     * @param color the color to activate
     */
    void activate(Color color);

    /**
     * Gets the active Color
     * @return active Color
     */
    Color getActive();

    /**
     * Show the active Color and stats about secrets
     */
    void info();

    /**
     * Creates key vaults for all Colors.
     * If a key vault already exists it will be dropped.
     */
    void initialize();

}
