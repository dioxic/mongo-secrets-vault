# MongoDB Secrets Tool

A tool to store encrypted secrets in MongoDB using client-side encryption.

There is a BLUE and a GREEN vault for storing secrets and each vault uses a different master key to encrypt/decrypt the data.

Secrets are always written to both the BLUE and GREEN vaults.

Secrets are read from the active vault.

The master key can be rotated for the inactive vault. The steps for this are:
1. Wipe the secrets and keys for the inactive vault
2. Change the master key for the inactive vault
3. Re-populate the inactive vault with secrets from the active vault (encrypted using the new key) 

## Usage

```
Usage: msv [-hV] [COMMAND]
MongoDB secrets CLI tool
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
  activate  Activate a Color
  init      Initializes vaults - drops existing vaults and secrets
  info      Shows information about the vault
  read      Read a secret
  rotate    Rotate the master key for the inactive color
  write     Write a secret
```
The master keys are not stored in plaintext. If a non-default master key was used



## Examples

Initializing vaults for the first time with default master keys

```
msv init
```

Initializing vaults for the first time with a specific key for the BLUE vault

```
msv init --blue-key myMasterKey
```

Writing a secret with using default master keys and generated secret id

```
> msv write mySecret
Writing secret to BLUE & GREEN vaults...
Secret written (id: 66bf42bfc56ef02927bbd6e0)
```

Writing a secret with using specific master keys and secret id

```
> msv write --secret-id secret1 --blue-key blueMaster --green-key greenMaster mySecret
Writing secret to BLUE & GREEN vaults...
Secret written (id: secret1)
```

Reading a secret from the active vault

```
> msv read secret1
Reading secret secret1 from GREEN...
secret1=mySecret
```

Rotating the inactive vault with a new key

```
> msv rotate myNewMasterKey
Rotating secrets in BLUE vault...
1 secrets rotated for BLUE
```