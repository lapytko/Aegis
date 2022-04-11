package com.beemdevelopment.aegis.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.ThemeMap;
import com.beemdevelopment.aegis.helpers.QrCodeAnalyzer;
import com.beemdevelopment.aegis.otp.YandexAuthInfo;
import com.beemdevelopment.aegis.otp.YandexAuthInfoException;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.zxing.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class YandexScannerActivity extends AegisActivity implements QrCodeAnalyzer.Listener {
    private ProcessCameraProvider _cameraProvider;
    private ListenableFuture<ProcessCameraProvider> _cameraProviderFuture;

    private List<Integer> _lenses;
    private int _currentLens;

    private Menu _menu;
    private ImageAnalysis _analysis;
    private PreviewView _previewView;
    private ExecutorService _executor;
    private String _code;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (abortIfOrphan(savedInstanceState)) {
            return;
        }
        setContentView(R.layout.activity_scanner);
        setSupportActionBar(findViewById(R.id.toolbar));

        _lenses = new ArrayList<>();
        _previewView = findViewById(R.id.preview_view);
        _executor = Executors.newSingleThreadExecutor();

        _cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        _cameraProviderFuture.addListener(() -> {
            try {
                _cameraProvider = _cameraProviderFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                // if we're to believe the Android documentation, this should never happen
                // https://developer.android.com/training/camerax/preview#check-provider
                throw new RuntimeException(e);
            }

            addCamera(CameraSelector.LENS_FACING_BACK);
            addCamera(CameraSelector.LENS_FACING_FRONT);
            if (_lenses.size() == 0) {
                Toast.makeText(this, getString(R.string.no_cameras_available), Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            _currentLens = _lenses.get(0);
            updateCameraIcon();

            bindPreview(_cameraProvider);
        }, ContextCompat.getMainExecutor(this));

            _code =(String) this.getIntent().getStringExtra("pin");
    }



    @Override
    protected void onDestroy() {
        _executor.shutdownNow();
        super.onDestroy();
    }

    @Override
    protected void onSetTheme() {
        setTheme(ThemeMap.FULLSCREEN);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        _menu = menu;
        getMenuInflater().inflate(R.menu.menu_scanner, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_camera) {
            unbindPreview(_cameraProvider);
            _currentLens = _currentLens == CameraSelector.LENS_FACING_BACK ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
            bindPreview(_cameraProvider);
            updateCameraIcon();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void addCamera(int lens) {
        try {
            CameraSelector camera = new CameraSelector.Builder().requireLensFacing(lens).build();
            if (_cameraProvider.hasCamera(camera)) {
                _lenses.add(lens);
            }
        } catch (CameraInfoUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void updateCameraIcon() {
        if (_menu != null) {
            MenuItem item = _menu.findItem(R.id.action_camera);
            boolean dual = _lenses.size() > 1;
            if (dual) {
                switch (_currentLens) {
                    case CameraSelector.LENS_FACING_BACK:
                        item.setIcon(R.drawable.ic_camera_front_24dp);
                        break;
                    case CameraSelector.LENS_FACING_FRONT:
                        item.setIcon(R.drawable.ic_camera_rear_24dp);
                        break;
                }
            }
            item.setVisible(dual);
        }
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(_previewView.getSurfaceProvider());

        CameraSelector selector = new CameraSelector.Builder()
                .requireLensFacing(_currentLens)
                .build();

        _analysis = new ImageAnalysis.Builder()
                .setTargetResolution(QrCodeAnalyzer.RESOLUTION)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        _analysis.setAnalyzer(_executor, new QrCodeAnalyzer(this));

        cameraProvider.bindToLifecycle(this, selector, preview, _analysis);
    }

    private void unbindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        _analysis = null;
        cameraProvider.unbindAll();
    }

    @Override
    public void onQrCodeDetected(Result result) {
        if (_analysis != null) {
            try {
                Uri uri = Uri.parse(result.getText().trim());
                    handleUri(uri);
                } catch (YandexAuthInfoException e) {
                e.printStackTrace();

                unbindPreview(_cameraProvider);

                Dialogs.showErrorDialog(this,
                        e.isPhoneFactor() ? R.string.read_qr_error_phonefactor : R.string.read_qr_error,
                        e, ((dialog, which) -> bindPreview(_cameraProvider)));
            }
        }
    }

    private void handleUri(Uri uri) throws YandexAuthInfoException {
        YandexAuthInfo info = YandexAuthInfo.parseUri(uri, _code);
        List<YandexAuthInfo> entries = new ArrayList<>();
        entries.add(new YandexAuthInfo(info));
        finish(entries);
    }

    private void finish(List<YandexAuthInfo> entries) {
        Intent intent = new Intent();
        intent.putExtra("magic", (ArrayList<YandexAuthInfo>) entries);
        setResult(RESULT_OK, intent);
        finish();
    }
}