package ru.nsu.nocode.server;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;

public class CertificateUtils {
    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static PrivateKey loadPrivateKeyPem(Path pemPath) throws Exception {
        try (Reader reader = Files.newBufferedReader(pemPath);
             PEMParser parser = new PEMParser(reader)) {
            Object obj = parser.readObject();
            JcaPEMKeyConverter conv = new JcaPEMKeyConverter().setProvider("BC");
            if (obj instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo pkInfo)
                return conv.getPrivateKey(pkInfo);
            else
                throw new IllegalArgumentException("Unsupported PEM object: " + obj.getClass());
        }
    }

    public static X509Certificate buildCertificate(String cn, PublicKey pub, PrivateKey issuerKey, String issuerDN) throws Exception {
        X500Name issuer = new X500Name(issuerDN);
        X500Name subject = new X500Name("CN=" + cn);
        BigInteger serial = new BigInteger(160, new SecureRandom());
        Date notBefore = new Date(System.currentTimeMillis() - 60000);
        Date notAfter = new Date(System.currentTimeMillis() + 365L * 24 * 3600 * 1000);

        var builder = new JcaX509v3CertificateBuilder(issuer, serial, notBefore, notAfter, subject, pub);
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(issuerKey);

        return new JcaX509CertificateConverter().setProvider("BC")
                .getCertificate(builder.build(signer));
    }

    public static KeyPair generateRSAKeyPair(int bits) throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(bits);
        return gen.generateKeyPair();
    }

    public static String toPem(Object obj) throws IOException {
        StringWriter sw = new StringWriter();
        try (JcaPEMWriter w = new JcaPEMWriter(sw)) {
            w.writeObject(obj);
            w.flush();
        }
        return sw.toString();
    }
}
