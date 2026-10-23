# ParkNote: private release signing

The Android keystore is **not** in Git. Do not upload the keystore or its passwords
to ChatGPT, GitHub issues, PRs, logs, or commit history.

## Initial setup (Windows PowerShell 7)

1. Download and run `ParkNote-Upload-SigningKey-v2.ps1` on your own computer.
2. The uploader automatically finds `.jks` in `%USERPROFILE%\\Downloads`. If
   several JKS files exist, select one by number, not by entering a path.
   Type only the alias and password(s) at local masked prompts.
3. The helper creates the `release-signing` environment restricted to protected branches.
4. The helper encrypts four environment secrets using GitHub CLI (plaintext stays on your computer):
   - `PARKNOTE_KEYSTORE_B64`: in-memory Base64 representation of the binary keystore;
   - `PARKNOTE_STORE_PASSWORD`: keystore password;
   - `PARKNOTE_KEY_ALIAS`: signing alias;
   - `PARKNOTE_KEY_PASSWORD`: key password.
5. Keep an offline backup of the original keystore **and** your alias/passwords.

### Approved public signing certificate

The first successful signed ParkNote 0.1.0 test APK established this certificate
SHA-256 fingerprint (public verification data, **not** a secret):

`f02571c40741e2cb071564f5b63fd3dc38a875d0eda11a8c42269ed635bc2a58`

The signed-build workflow rejects any APK signed by a different certificate.
This preserves update compatibility when installing beta builds over an existing
installation. Never replace the private JKS without an explicit signing-key
rotation/migration plan.

The GitHub CLI encrypts secrets before uploading. Base64 is merely a transport
encoding, not encryption. Secret values cannot be retrieved through the GitHub
Secrets API after they have been set.

## Signed test build

Signed builds can be launched from protected `main` in either of two ways:
- Manually: `gh workflow run signed-build.yml -R StanleyLl0yd/parknote --ref main`.
- After the owner confirms `SIGNING_SECRETS_VERIFIED`, the maintainer creates a
  pull request changing the inert `.github/signed-build-request.txt` marker,
  waits for all mandatory CI checks and merges it through branch protection.
  The resulting `main` push automatically triggers exactly one signed build.

Changing application code alone does not trigger signed builds. Never add a
keystore, password, encoded secret, certificate private key, or personal details
to the marker file. Do not merge a signing request before confirmation.

The workflow has read-only GitHub permissions, checks out only main, restores the
keystore into runner temporary storage, signs APK/AAB, verifies both signatures,
checks 16 KiB ELF/APK alignment and uploads the signed APK/AAB for 7 days.
The temporary key is removed at the end of the job (runner disposal is the
additional boundary). Android CI and pull-request jobs never receive the
signing secrets and continue to produce unsigned release bundles.

The published artifact and its signature are *not* confidential; do not confuse
the signed test build with approval to publish. Physical device testing,
release acceptance, and explicit RuStore/Google Play authorization remain open.

### Trust boundary

Only authorized maintainers should be able to modify workflows on main. This
environment is restricted to protected branches, but that alone is **not**
a substitute for required review of workflow changes or environment reviewers.
For stronger separation, require a designated second reviewer for
`release-signing` deployments, and prohibit unreviewed workflow changes.
