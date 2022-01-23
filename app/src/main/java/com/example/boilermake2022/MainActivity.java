package com.example.boilermake2022;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.w3c.dom.Document;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Boilermake2022";
    private static final String CHANNEL_ID = "perish_notification_channel";
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseStorage storage = FirebaseStorage.getInstance();

    // One Button
    Button BSelectImage;

    // One Preview Image
    ImageView IVPreviewImage;

    TextView statusReport;

    // constant to compare
    // the activity result code
    int SELECT_PICTURE = 200;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private ArrayList<String> lines = new ArrayList<String>();
    private ArrayList<String> foods = new ArrayList<String>();

    private StorageReference foodCSVRef;
    private String UIUD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

       UIUD = "AHIOHFIODH";

        createNotificationChannel();

        setContentView(R.layout.activity_main);

        StorageReference storageRef = storage.getReference();
        foodCSVRef = storageRef.child("food.csv");

        try {
            File localFoodCSV = File.createTempFile("food", "csv");
            foodCSVRef.getFile(localFoodCSV).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    // Local temp file has been created
                    try {
                        Scanner scan = new Scanner(localFoodCSV);
                        scan.useDelimiter(",");
                        while(scan.hasNext()) {
                            foods.add(scan.next().toLowerCase());
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle any errors
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        // register the UI widgets with their appropriate IDs
        BSelectImage = findViewById(R.id.BSelectImage);
        IVPreviewImage = findViewById(R.id.IVPreviewImage);
        statusReport = findViewById(R.id.statusReport);

        statusReport.setText("Foods Found:\n");

        // handle the Choose Image button to trigger
        // the image chooser function
        BSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageChooser();
            }
        });

    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

        }
    }

    // this function is triggered when
    // the Select Image Button is clicked
    void imageChooser() {

        // create an instance of the
        // intent of the type image
        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);

        // pass the constant to compare it
        // with the returned requestCode
        startActivityForResult(Intent.createChooser(i, "Select Picture"), SELECT_PICTURE);
    }

    // this function is triggered when user
    // selects the image from the imageChooser
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            // compare the resultCode with the
            // SELECT_PICTURE constant
            if (requestCode == SELECT_PICTURE) {
                // Get the url of the image from data
                Uri selectedImageUri = data.getData();
                TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

                if (null != selectedImageUri) {
                    // update the preview image in the layout
                    IVPreviewImage.setImageURI(selectedImageUri);
                    try {
                        InputImage uploaded = InputImage.fromFilePath(this.getApplicationContext(), selectedImageUri);
                        Task<Text> result =
                                recognizer.process(uploaded)
                                        .addOnSuccessListener(new OnSuccessListener<Text>() {
                                            @Override
                                            public void onSuccess(Text visionText) {
                                                // Task completed successfully
                                                // ...
                                                System.out.println("AI Worked!");
                                            }
                                        })
                                        .addOnFailureListener(
                                                new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        // Task failed with an exception
                                                        // ...
                                                        e.printStackTrace();
                                                    }
                                                });
                        while(!result.isComplete()) {

                        }
                        ArrayList<String> lines = new ArrayList<String>();
                        String resultText = result.getResult().getText();
                        for (Text.TextBlock block : result.getResult().getTextBlocks()) {
                            String blockText = block.getText();
                            Point[] blockCornerPoints = block.getCornerPoints();
                            Rect blockFrame = block.getBoundingBox();
                            for (Text.Line line : block.getLines()) {
                                String lineText = line.getText();
                                Point[] lineCornerPoints = line.getCornerPoints();
                                Rect lineFrame = line.getBoundingBox();
                                String accumulator = "";
                                for (Text.Element element : line.getElements()) {
                                    String elementText = element.getText();
                                    accumulator += elementText + " ";
                                    Point[] elementCornerPoints = element.getCornerPoints();
                                    Rect elementFrame = element.getBoundingBox();
                                }
                                lines.add(accumulator);
                                accumulator = "";

                            }
                        }

                        preprocessLines(lines);


                        for(int i = 0; i < lines.size(); i++) {
                            String line = lines.get(i);
                            if(!isFood(line)) {
                                lines.remove(i);
                                i--;
                            }
                        }

                        System.out.println(lines);

                        //Create new notification for user because we know the expiration date.
                        // Create an explicit intent for an Activity in your app
                        Intent intent = new Intent(this, AlertDialog.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.notification_icon)
                                .setContentTitle("Perishable Food Alert!")
                                .setContentText("You have  days left!")
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                // Set the intent that will fire when the user taps the notification
                                .setContentIntent(pendingIntent)
                                .setAutoCancel(true);

                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

                        HashMap<String, Object> foodExpirationDays = new HashMap<String, Object>();
                        db.collection("expiration")
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()) {
                                            for (QueryDocumentSnapshot foodExpiration : task.getResult()) {
                                                HashMap<String, Object> hashmap = (HashMap<String, Object>) foodExpiration.getData();
                                                foodExpirationDays.put(foodExpiration.getId(), hashmap.get("room"));
                                                System.out.println("Expiration Date for " + foodExpiration.getId() + " is " + hashmap.get("room"));
                                            }


                                            HashMap<String, Object> foodsToBeAdded = new HashMap<String, Object>();
                                            CollectionReference userCollectionReference = db.collection("users");
                                            int curId = 0;
                                            for(String line : lines) {
                                                System.out.println(foodExpirationDays.get(line) + " Food Expiration Days");
                                                if(foodExpirationDays.get(line) != null) {
                                                    // Create the document in firestore
                                                    foodsToBeAdded.put(line, foodExpirationDays.get(line));
                                                    System.out.println("Elite: Added " + foodExpirationDays.get(line) + " from " + line);
                                                    statusReport.setText(statusReport.getText() + line + " Expires " + foodExpirationDays.get(line) + " days from now" + "\n");
                                                    notificationManager.notify(curId, builder.build());
                                                }
                                                curId++;
                                            }
                                            System.out.println("UIUD: " + foodsToBeAdded.get("apple"));
                                            HashMap<String, Object> generalHash = new HashMap<String, Object>();
                                            generalHash.put(UIUD, foodsToBeAdded);
                                            userCollectionReference.document(UIUD).set(foodsToBeAdded).addOnSuccessListener(new OnSuccessListener() {
                                                @Override
                                                public void onSuccess(Object o) {
                                                    System.out.println("Added " + userCollectionReference.document(UIUD) + " to collection " + userCollectionReference.getId());
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.w(TAG, e.getLocalizedMessage());
                                                }
                                            });
                                        } else {

                                        }
                                    }
                                });



                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void preprocessLines(ArrayList<String> lines) {
        for(int i = 0; i < lines.size(); i++) {
            String original = lines.get(i);
            String removePunctuation = original.replaceAll("[^a-zA-Z ]", "").toLowerCase();
            int j = removePunctuation.length()-1;
            while(j >= 0 && removePunctuation.charAt(j) == ' ') {
                j--;
            }
            lines.set(i, removePunctuation.substring(0, j+1));
        }
    }

    private boolean isFood(String line) {
        for(String food: foods) {
            if(food.contains(line)) {
                return true;
            }
        }
        return false;
    }
}