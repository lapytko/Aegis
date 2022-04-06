package com.beemdevelopment.aegis.helpers;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import com.beemdevelopment.aegis.crypto.KeyStoreHandle;
import com.beemdevelopment.aegis.crypto.KeyStoreHandleException;
import com.beemdevelopment.aegis.vault.slots.BiometricSlot;
import com.beemdevelopment.aegis.vault.slots.Slot;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

/**
 * A class that can prepare initialization of a BiometricSlot by generating a new
 * key in the Android KeyStore and authenticating a cipher for it through a
 * BiometricPrompt.
 */
public class BiometricSlotInitializer extends BiometricPrompt.AuthenticationCallback {
    private BiometricSlot _slot;
    private Listener _listener;
    private BiometricPrompt _prompt;
    private Cipher _cipher ;
    private  SecretKey _key;
    private KeyStoreHandle _keyStore;


    public BiometricSlotInitializer(Fragment fragment, Listener listener) {
        _listener = listener;
        _prompt = new BiometricPrompt(fragment, new UiThreadExecutor(), this);
    }

    public BiometricSlotInitializer(FragmentActivity activity, Listener listener) {
        _listener = listener;
        _prompt = new BiometricPrompt(activity, new UiThreadExecutor(), this);
    }

    /**
     * Generates a new key in the Android KeyStore for the new BiometricSlot,
     * initializes a cipher with it and shows a BiometricPrompt to the user for
     * authentication. If authentication is successful, the new slot will be
     * initialized and delivered back through the listener.
     */
    public void authenticate(BiometricPrompt.PromptInfo info) {
        if (_slot != null) {
            throw new IllegalStateException("Biometric authentication already in progress");
        }

        try {
            _prompt.authenticate(info);
            _keyStore = new KeyStoreHandle();
        } catch (KeyStoreHandleException e) {
            fail(e);
            return;
        }

        _slot = new BiometricSlot();

        try {
            _prompt.authenticate(info, new BiometricPrompt.CryptoObject(_cipher));
        } catch (Exception e) {
            fail(e);
            return;
        }

    }

    /**
     * Cancels the BiometricPrompt and resets the state of the initializer. It will
     * also attempt to delete the previously generated Android KeyStore key.
     */
    public void cancelAuthentication() {
        if (_slot == null) {
            throw new IllegalStateException("Biometric authentication not in progress");
        }
        reset();
        _prompt.cancelAuthentication();
    }

    private void reset() {
        if (_slot != null) {
            try {
                // clean up the unused KeyStore key
                // this is non-critical, so just fail silently if an error occurs
                String uuid = _slot.getUUID().toString();
                KeyStoreHandle keyStore = new KeyStoreHandle();
                if (keyStore.containsKey(uuid)) {
                    keyStore.deleteKey(uuid);
                }
            } catch (KeyStoreHandleException e) {
                e.printStackTrace();
            }

            _slot = null;
        }
    }

    private void fail(int errorCode, CharSequence errString) {
        reset();
        _listener.onSlotInitializationFailed(errorCode, errString);
    }

    private void fail(Exception e) {
        e.printStackTrace();
        fail(0, e.toString());
    }

    @Override
    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
        super.onAuthenticationError(errorCode, errString);
        fail(errorCode, errString.toString());
    }

    @Override
    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
        super.onAuthenticationSucceeded(result);
        try {
            _key = _keyStore.generateKey(_slot.getUUID().toString());
            _cipher = Slot.createEncryptCipher(_key);
            _listener.onInitializeSlot(_slot, _cipher);
        } catch (Exception e){
            fail(e);
            e.setStackTrace(e.getStackTrace());
        }
    }

    @Override
    public void onAuthenticationFailed() {
        super.onAuthenticationFailed();
    }

    public interface Listener {
        void onInitializeSlot(BiometricSlot slot, Cipher cipher);
        void onSlotInitializationFailed(int errorCode, @NonNull CharSequence errString);
    }
}
