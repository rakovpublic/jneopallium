# Publishing to Maven Central

The build is configured for Maven Central. Everything Central needs but a normal build does
not — sources, javadoc, PGP signatures and the portal upload — lives in the **`release`
profile** in the root `pom.xml`, so `mvn install`, `mvn test` and the GitLab package-registry
deploy behave exactly as they did before.

```bash
mvn -Prelease deploy
```

What follows is the part that is not in the repository: the account, the namespace, the signing
key and the token. Those are tied to the project owner's identity and can only be done by them.

---

## 1. What is already done

| Central requirement | Where |
|---|---|
| `name`, `description` on every published artifact | each module's `pom.xml` (not inherited in Maven, so declared per module) |
| `url`, `licenses`, `developers`, `scm` | root `pom.xml`, inherited by every module |
| groupId under a verifiable namespace | `io.github.rakovpublic.jneopallium` |
| sources jar | `maven-source-plugin` in the `release` profile |
| javadoc jar | `maven-javadoc-plugin` in the `release` profile (`doclint` off — Central requires the jar to exist, not that every symbol is documented) |
| PGP signatures | `maven-gpg-plugin` in the `release` profile |
| upload | `central-publishing-maven-plugin`, `autoPublish=false` so nothing goes live without a confirmation |
| scope | demos and `integration-tests` set `maven.deploy.skip` **inside the `release` profile only**, so they still publish to GitLab |

## 2. What only you can do

### 2.1 Central account

Register at <https://central.sonatype.com> (sign in with GitHub). This is the current path —
the old OSSRH / `oss.sonatype.org` route was retired.

### 2.2 Verify the `io.github.rakovpublic` namespace

In the portal, add the namespace `io.github.rakovpublic`. It will give you a verification code
such as `abc123xyz`. Create a **public repository with exactly that name** under
<https://github.com/rakovpublic>, then press Verify. The repository can be deleted afterwards.

Verification is what entitles the account to publish anything starting with
`io.github.rakovpublic`, which is why the groupId is
`io.github.rakovpublic.jneopallium` — a subgroup of the verified namespace.

### 2.3 Signing key

Central requires every artifact to be signed, and the public key to be resolvable.

```bash
gpg --gen-key                                   # RSA, 4096, your name and email
gpg --list-secret-keys --keyid-format=long      # note the key id
gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
```

Keep the private key and its passphrase to yourself — they are not something to paste into a
chat, a pom, or a repository. The build reads the passphrase from the gpg agent or from the
`MAVEN_GPG_PASSPHRASE` environment variable.

Back the key up. Losing it means future releases cannot be signed with the same identity.

Two things that will bite otherwise:

- **If the keyring holds more than one secret key**, `maven-gpg-plugin` signs with gpg's default,
  which is the first one — not necessarily the one you meant. Either delete the keys you are not
  using (`gpg --delete-secret-keys <FPR>` then `gpg --delete-keys <FPR>`), set `default-key` in
  `gpg.conf`, or pass `-Dgpg.keyname=<FPR>` on every release.
- **Signing needs to reach a pinentry.** A release run from a non-interactive shell fails with
  `gpg: signing failed: No pinentry`. Run it from an interactive terminal, or export the
  passphrase for the duration of the command:

  ```bash
  export MAVEN_GPG_PASSPHRASE='...'      # not stored anywhere, just this shell
  ```

  The profile already passes `--pinentry-mode loopback`, which is what makes the environment
  variable work.

### 2.4 Portal token

In the portal: *Account → Generate User Token*. It gives a username/password pair. Put it in
`~/.m2/settings.xml` under the server id the plugin expects:

```xml
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>TOKEN_USERNAME</username>
      <password>TOKEN_PASSWORD</password>
    </server>
  </servers>
</settings>
```

Do not commit this file.

---

## 3. Releasing

Central does not accept `-SNAPSHOT` for a release, and a version can never be re-published, so
the version is set explicitly for the release and then moved back to a snapshot.

```bash
# 1. set the release version across the reactor
mvn versions:set -DnewVersion=1.0.0 -DprocessAllModules=true
mvn versions:commit

# 2. build, sign and upload
export MAVEN_GPG_PASSPHRASE='...'      # or let the gpg agent prompt
mvn -Prelease clean deploy

# 3. confirm in the portal, then tag
git tag -a v1.0.0 -m "1.0.0"
git push origin v1.0.0

# 4. back to development
mvn versions:set -DnewVersion=1.0.1-SNAPSHOT -DprocessAllModules=true
mvn versions:commit
```

With `autoPublish=false` the upload lands in the portal as a *validated* deployment and waits
there. Look at it, then press **Publish**. It reaches `repo1.maven.org` within about half an
hour and appears on search.maven.org later the same day.

If validation fails the portal says which rule was broken and the deployment can be dropped and
re-uploaded under the same version — nothing is public until you publish it.

## 4. What gets published

| Published | Skipped |
|---|---|
| `wrapper` (root POM), `worker-core`, `agi-base`, `bridge-api`, `master` | `demos` and all `demo-*` modules |
| `domains` + the 14 `domain-*` modules | `integration-tests` |
| `bridges` + the 16 `bridge-*` modules | |

Aggregator POMs have to be published because the modules inherit from them; a consumer cannot
resolve `worker-core` without `wrapper`.

`master` is a war rather than a library. It is published because it is the cluster coordinator
and part of the deliverable, not a demo. To leave it out, add the same `release`-profile
`maven.deploy.skip` block to `master/pom.xml` that `demos/pom.xml` uses.

## 5. Consuming the result

```xml
<dependency>
    <groupId>io.github.rakovpublic.jneopallium</groupId>
    <artifactId>worker-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 6. Notes

- **The groupId changed** from `com.rakovpublic.jneopallium` to
  `io.github.rakovpublic.jneopallium`. That is a consequence of verifying via GitHub rather than
  a domain. Publishing under `com.rakovpublic` instead would require control of
  `rakovpublic.com` and a DNS TXT record; if you own it, the groupId can be changed back and
  everything else here still applies.
- **The GitLab publish keeps working** and now uses the new groupId. Nothing in
  `.gitlab-ci.yml`, the root `distributionManagement` or the deploy command changed.
- **Javadoc lint is off.** Turning `doclint` back on is worth doing eventually, but it fails on
  a lot of pre-existing javadoc and would block a release today.
