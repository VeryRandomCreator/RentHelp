package com.veryrandomcreator.tenanthelp;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.spec.ECGenParameterSpec;

// Used Google Gemini for most of this
public class HardwareKeyManager {
    private static final String KEY_ALIAS = "RentalDisputeKey";

    public static void generateAttestedKey(byte[] challenge) throws Exception {
        // 1. Specify the Android KeyStore and the Elliptic Curve algorithm
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");

        // 2. Build the exact specifications for the hardware key
        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)

                // This is the magic line. Passing a challenge tells the TEE to
                // generate an X.509 Attestation Certificate proving this key is hardware-backed.
                .setAttestationChallenge(challenge)

                // Optional: Force StrongBox (dedicated security chip) if available on the device
                // .setIsStrongBoxBacked(true)

                // Ensure the user doesn't need to authenticate (fingerprint/PIN) just to sign the PDF
                .setUserAuthenticationRequired(false);

        // 3. Initialize the generator and create the key inside the secure silicon
        keyPairGenerator.initialize(builder.build());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
    }

    private static Certificate[] getCertificateChain() throws Exception {
        // Load the Android KeyStore
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        // Retrieve the certificate chain generated during key creation
        Certificate[] certChain = keyStore.getCertificateChain(KEY_ALIAS);

        if (certChain == null || certChain.length == 0) {
            throw new Exception("Certificate chain not found. Key generation may have failed.");
        }

        return certChain;
    }

    // save as .pem
    public static byte[] getCertificateChainBytes() throws Exception {
        StringBuilder pemBuilder = new StringBuilder();
        Certificate[] certificateChain = getCertificateChain();

        for (Certificate cert : certificateChain) {
            String base64Cert = Base64.encodeToString(cert.getEncoded(), Base64.NO_WRAP);

            // 2. Wrap it in the standard PEM headers
            pemBuilder.append("-----BEGIN CERTIFICATE-----\n");
            pemBuilder.append(base64Cert).append("\n");
            pemBuilder.append("-----END CERTIFICATE-----\n");
        }

        return pemBuilder.toString().getBytes();
    }

    public static byte[] signPdfDocument(byte[] pdfBytes) throws Exception {

        // 1. Load the Android KeyStore
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        // 2. Retrieve the Private Key reference
        // Note: This does NOT extract the private key from the hardware.
        // It only gets a handle that allows the app to ask the TEE to use it.
        KeyStore.Entry entry = keyStore.getEntry(KEY_ALIAS, null);
        if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
            throw new Exception("Private key not found. Has the key been generated?");
        }
        PrivateKey hardwarePrivateKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();

        // 3. Initialize the Signature algorithm
        // We used Elliptic Curve (secp256r1) and SHA-256 in Step 1,
        // so the corresponding signature algorithm is SHA256withECDSA.
        Signature ecdsaSignature = Signature.getInstance("SHA256withECDSA");
        ecdsaSignature.initSign(hardwarePrivateKey);

        // 4. Feed the PDF data into the signing engine
        ecdsaSignature.update(pdfBytes);

        // 5. Generate and return the final cryptographic signature
        byte[] finalSignature = ecdsaSignature.sign();

        return finalSignature;
    }
}
