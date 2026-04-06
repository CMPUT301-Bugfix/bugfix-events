package com.example.eventlotterysystem;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import android.widget.ImageView;
import android.graphics.Bitmap;
import com.google.zxing.*;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import androidx.appcompat.app.AppCompatActivity;

/**
 * The class responsible for create a unique QR code for an event
 * <p>
 * This class retrieves an "EVENT_ID" from the intent, format it into a custom URI scheme,
 * and generate the QR code image using the {@link BarcodeEncoder}.
 */
public class QRCode extends AppCompatActivity {
    /**
     * This is the creation of the Activity
     * it creates and displays the QR code for the current Event
     * @param saveInstanceState
     * the saved state of the Activity so that the screen is not reset
     */
    @Override
    protected void onCreate(Bundle saveInstanceState){
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_qr_code);

        // Retrieves event ID passed from the previous activity and format it into URI
        String rawEventId = getIntent().getStringExtra("EVENT_ID");
        if (TextUtils.isEmpty(rawEventId)) {
            rawEventId = getIntent().getStringExtra("Event_ID");
        }
        if (TextUtils.isEmpty(rawEventId)) {
            Toast.makeText(this, R.string.missing_event_id, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String eventID = "myapp://event?id=" + rawEventId;

        // Set up Back button to close the QR code generation page
        findViewById(R.id.qrCodeBack).setOnClickListener(v -> finish());

        ImageView qrCode = findViewById(R.id.qrCode);

        try{
            // Initialize the ZXing BarcodeEncoder to handle bitmap generation
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();

            // Create 400 by 400 QR code bitmap
            Bitmap bitmap = barcodeEncoder.encodeBitmap(eventID, BarcodeFormat.QR_CODE, 400, 400);

            // Display generated QR code in imageView
            qrCode.setImageBitmap(bitmap);
        } catch (Exception e) {
            // Log error if QR code failed to generate
            e.printStackTrace();
        }

    }
}
