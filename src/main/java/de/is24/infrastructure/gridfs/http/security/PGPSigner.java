package de.is24.infrastructure.gridfs.http.security;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.Iterator;

import static org.apache.commons.io.IOUtils.closeQuietly;

@Service
public class PGPSigner {

  static {
    if (Security.getProvider("BC") == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  public static final int BUFFER_SIZE = 1024;

  private final Resource keyRingResource;
  private final String password;
  private final PGPPrivateKey privateKey;
  private final int algorithm;

  @Autowired
  public PGPSigner(@Value("${security.signature.private.keyfile:classpath:/gpg/secring.gpg}") Resource keyRingResource,
                   @Value("${security.signature.private.password:yum-repo-server}") String password) {
    this.keyRingResource = keyRingResource;
    this.password = password;

    if (isActive()) {
      PGPSecretKey secretKey = findFirstSecretKey(readKeyRings(keyRingResource));
      algorithm = secretKey.getPublicKey().getAlgorithm();
      privateKey = readKey(secretKey);
    } else {
      privateKey = null;
      algorithm = 0;
    }
  }

  private PGPPrivateKey readKey(PGPSecretKey secretKey) {
    try {
      PBESecretKeyDecryptor secretKeyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(this.password.toCharArray());
      return secretKey.extractPrivateKey(secretKeyDecryptor);
    } catch (PGPException e) {
      throw new IllegalArgumentException("Could not read private key from key ring file", e);
    }
  }

  private PGPSecretKey findFirstSecretKey(PGPSecretKeyRingCollection keyRings) {
    @SuppressWarnings("unchecked")
    Iterator<PGPSecretKeyRing> iter = keyRings.getKeyRings();
    while (iter.hasNext()) {
      PGPSecretKeyRing keyRing = iter.next();

      @SuppressWarnings("unchecked")
      Iterator<PGPSecretKey> keyIter = keyRing.getSecretKeys();
      while (keyIter.hasNext()) {
        PGPSecretKey key = keyIter.next();
        if (key.isSigningKey()) {
          return key;
        }
      }
    }

    throw new IllegalArgumentException("Can't find signing key in key rings.");
  }

  private PGPSecretKeyRingCollection readKeyRings(Resource resource) {
    try {
      InputStream keyInputStream = new BufferedInputStream(resource.getInputStream());
      InputStream decoderStream = PGPUtil.getDecoderStream(keyInputStream);
      try {
        return new PGPSecretKeyRingCollection(decoderStream);

      } finally {
        closeQuietly(decoderStream);
        closeQuietly(keyInputStream);
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not read key ring file: " + keyRingResource, e);
    } catch (PGPException e) {
      throw new IllegalArgumentException("Could not extract key rings.", e);
    }
  }

  public boolean isActive() {
    return keyRingResource != null;
  }

  public byte[] sign(byte[] content) {
    if (!isActive()) {
      throw new IllegalStateException("Key file not given during initialisation.");
    }

    ByteArrayOutputStream buffer = new ByteArrayOutputStream(BUFFER_SIZE);
    BCPGOutputStream outputStream = new BCPGOutputStream(new ArmoredOutputStream(new BufferedOutputStream(buffer)));
    try {
      PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(new JcaPGPContentSignerBuilder(algorithm, PGPUtil.SHA1).setProvider("BC"));
      signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, privateKey);
      signatureGenerator.update(content);
      signatureGenerator.generate().encode(outputStream);
    } catch (Exception e) {
      throw new IllegalStateException("Could not sign content.", e);
    } finally {
      closeQuietly(outputStream);
    }
    return buffer.toByteArray();
  }


}
