package android.example.cameraxapp;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Size;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.extensions.HdrImageCaptureExtender;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

  private Executor executor = Executors.newSingleThreadExecutor();
  private int REQUEST_CODE_PERMISSIONS = 1001;
  private final String[] REQUIRED_PERMISSIONS =
      new String[] {"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};

  PreviewView previewView;
  ImageView captureImage;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    previewView = findViewById(R.id.preview);
    captureImage = findViewById(R.id.captureImg);

    if (allPermissionsGranted()) {
      startCamera();
    } else {
      ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
    }
  }

  private void startCamera() {
    final ListenableFuture<ProcessCameraProvider> cameraProvideFuture =
        ProcessCameraProvider.getInstance(this);
    cameraProvideFuture.addListener(
        () -> {
          try {
            ProcessCameraProvider cameraProvider;
            cameraProvider = cameraProvideFuture.get();
            bindPreview(cameraProvider);
          } catch (InterruptedException | ExecutionException | CameraAccessException e) {
            // This should never be reached.
          }
        },
        ContextCompat.getMainExecutor(this));
  }

  private void bindPreview(@NonNull ProcessCameraProvider cameraProvider)
      throws CameraAccessException {
    Size targetResolution = new Size(960, 720);
    Preview preview = new Preview.Builder().setTargetResolution(targetResolution).build();
//    CameraSelector cameraSelector =
//        new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
      CameraSelector cameraSelector =
              new CameraSelector.Builder().build();
    //      CameraSelector cameraSelector =
    //              new CameraSelector.Builder().build();
    ImageAnalysis imageAnalysis =
        new ImageAnalysis.Builder()
            .setTargetResolution(targetResolution)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build();
    // Image analysis sample.
    imageAnalysis.setAnalyzer(
        executor,
        (image) -> {
          System.out.println("Image height: " + image.getHeight());
          System.out.println("Image width: " + image.getWidth());
          image.close();
        });

    ImageCapture.Builder imageCaptureBuilder =
        new ImageCapture.Builder().setTargetResolution(targetResolution);
    // Use HDR if available.
    HdrImageCaptureExtender hdrImageCaptureExtender =
        HdrImageCaptureExtender.create(imageCaptureBuilder);
    if (hdrImageCaptureExtender.isExtensionAvailable(cameraSelector)) {
      hdrImageCaptureExtender.enableExtension(cameraSelector);
    }

    final ImageCapture imageCapture =
        imageCaptureBuilder
            .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())
            .build();

    preview.setSurfaceProvider(previewView.getSurfaceProvider());

    Camera camera =
        cameraProvider.bindToLifecycle(
            (LifecycleOwner) this, cameraSelector, preview, imageAnalysis, imageCapture);

    //    captureImage.setOnClickListener(
    //        (unused) -> {
    //          SimpleDateFormat dataFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
    //          File file = new File(getBatchDirectoryName(), dataFormat.format(new Date()) +
    // ".jpg");
    //          ImageCapture.OutputFileOptions outputFileOptions =
    //              new ImageCapture.OutputFileOptions.Builder(file).build();
    //          imageCapture.takePicture(
    //              outputFileOptions,
    //              executor,
    //              new ImageCapture.OnImageSavedCallback() {
    //                @Override
    //                public void onImageSaved(
    //                    @NonNull ImageCapture.OutputFileResults outputFileResults) {
    //                  new Handler()
    //                      .post(
    //                          () -> {
    //                            Toast.makeText(
    //                                    MainActivity.this,
    //                                    "Image Saved successfully",
    //                                    Toast.LENGTH_SHORT)
    //                                .show();
    //                          });
    //                }
    //
    //                @Override
    //                public void onError(@NonNull ImageCaptureException exception) {
    //                  exception.printStackTrace();
    //                }
    //              });
    //        });
    captureImage.setOnClickListener(
        (unused) -> {
          File dir =
              new File(
                  Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                  "CameraXApp");
          if (dir.exists() || dir.mkdirs()) {
            SimpleDateFormat dataFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
            File file = new File(dir, dataFormat.format(new Date()) + ".jpg");

            ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(file).build();

            imageCapture.takePicture(
                outputFileOptions,
                executor,
                new ImageCapture.OnImageSavedCallback() {
                  @Override
                  public void onImageSaved(
                      @NonNull ImageCapture.OutputFileResults outputFileResults) {
                    new Handler()
                        .post(
                            () -> {
                              Toast.makeText(
                                      MainActivity.this,
                                      "Image Saved successfully",
                                      Toast.LENGTH_SHORT)
                                  .show();
                            });
                  }

                  @Override
                  public void onError(@NonNull ImageCaptureException exception) {
                    exception.printStackTrace();
                  }
                });
          }
        });
    // Show all supported output sizes.
    CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    for (String cameraId : cameraManager.getCameraIdList()) {
      CameraCharacteristics cameraCharacteristics =
          cameraManager.getCameraCharacteristics(cameraId);
      StreamConfigurationMap configMap =
          cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
      Size[] outputSizes = configMap.getOutputSizes(SurfaceTexture.class);
      for (Size size : outputSizes) {
        System.out.println("Camera " + cameraId + " output size: " + String.valueOf(size));
      }
    }
  }

  private String getBatchDirectoryName() {
    String appFolderPath = Environment.getExternalStorageDirectory().toString() + "/images";
    File dir = new File(appFolderPath);
    if (!dir.exists()) {
      dir.mkdirs();
    }
    return appFolderPath;
  }

  private boolean allPermissionsGranted() {
    for (String permission : REQUIRED_PERMISSIONS) {
      if (ContextCompat.checkSelfPermission(this, permission)
          != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == REQUEST_CODE_PERMISSIONS) {
      if (allPermissionsGranted()) {
        startCamera();
      } else {
        Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
        this.finish();
      }
    }
  }
}
