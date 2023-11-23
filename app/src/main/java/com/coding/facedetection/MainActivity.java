package com.coding.facedetection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    Button button;
    TextView textView;
    ImageView imageView;

    private final static int REQUEST_IMAGE_CAPTURE=124;
    InputImage firebaseVision;
    FaceDetection visionFaceDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button=findViewById(R.id.camera_btn);
        textView=findViewById(R.id.text1);
        imageView=findViewById(R.id.imageView);

        FirebaseApp.initializeApp(this);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OpenFile();
            }
        });
        Toast.makeText(this, "App is started", Toast.LENGTH_SHORT).show();
    }

    private void OpenFile(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(intent.resolveActivity(getPackageManager())!=null){
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        } else{
            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        Bundle bundle =data.getExtras();
        Bitmap bitmap=(Bitmap) bundle.get("data");

        FaceDetectionProcess(bitmap);
        Toast.makeText(this, "Success!!", Toast.LENGTH_SHORT).show();
    }


    public void FaceDetectionProcess(Bitmap bitmap){
        textView.setText("Processing Image......");
        final StringBuilder builder = new StringBuilder();
        BitmapDrawable drawable =(BitmapDrawable) imageView.getDrawable();

        InputImage image= InputImage.fromBitmap(bitmap,0);
        FaceDetectorOptions highAccuracyOpt=new FaceDetectorOptions.
                Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking().build();

        FaceDetector detector = FaceDetection.getClient(highAccuracyOpt);


        Task<List<Face>> result =detector.process(image);
        result.addOnSuccessListener(new OnSuccessListener<List<Face>>() {
            @Override
            public void onSuccess(List<Face> faces) {
                if (faces.size()!=0){
                    if(faces.size()==1){
                        builder.append(faces.size()+"Face Detected\n\n");
                    }else if(faces.size()>1){
                        builder.append(faces.size()+"Faces Detected\n\n");
                    }
                }
                for(Face face: faces){
                    //Tilting and rotating propability
                    int id=face.getTrackingId();
                    float rotY=face.getHeadEulerAngleY();
                    float rotZ=face.getHeadEulerAngleZ();

                    builder.append("1.Face Tracking ID ["+id+"]\n");
                    builder.append("2.Head Rotation to Right ["+String.format("%.1f", rotY)+"deg.]\n");
                    builder.append("3.Head Tilted Sideways ["+String.format("%.1f", rotZ)+"deg]\n");

                    //Smilling
                    if(face.getSmilingProbability()>0){
                        float smillingProbability=face.getSmilingProbability();
                        builder.append("4.Smilling Probability ["
                                +String.format("%.1f",smillingProbability)+"]\n");
                    }
                    //left eye open probability
                    if(face.getLeftEyeOpenProbability()>0){
                        float leftEye=face.getLeftEyeOpenProbability();
                        builder.append("4.Left Eye Probability ["
                                +String.format("%.1f",leftEye)+"]\n");
                    }
                    //right eye open
                    if(face.getRightEyeOpenProbability()>0){
                        float rightEye=face.getRightEyeOpenProbability();
                        builder.append("4.Right Eye open Probability ["
                                +String.format("%.1f",rightEye)+"]\n");
                    }
                    builder.append("\n");
                }
                ShowDetection("Face Detection", builder, true);
            }
        });

        result.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                StringBuilder builder1 =new StringBuilder();
                builder1.append("Sorry!! There is an error");
                ShowDetection("Face Detection",builder,false);
            }
        });


    }

    public void ShowDetection(final  String title, final StringBuilder builder, boolean success ){
        if(success==true){
            textView.setText(null);
            textView.setMovementMethod(new ScrollingMovementMethod());

            if(builder.length()!=0){
                textView.append(builder);
                if(title.substring(0,title.indexOf(' ')).equalsIgnoreCase("OCR")){
                    textView.append("\n(Hold the text to copy it)");

                }
                else{
                    textView.append("(Hold the text to copy it)");
                }
                textView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        ClipboardManager clipboard=(ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip=ClipData.newPlainText(title, builder);
                        clipboard.setPrimaryClip(clip);
                        return true;
                    }
                });
            }else{
                textView.append(title.substring(0,title.indexOf(' '))+"Failed to find anything!");
            }
        }else if(success==false){
            textView.setText(null);
            textView.setMovementMethod(new ScrollingMovementMethod());
            textView.append(builder);
        }
    }
}