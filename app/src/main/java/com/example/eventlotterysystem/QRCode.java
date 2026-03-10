package com.example.eventlotterysystem;

import android.os.Bundle;
import android.widget.ImageView;
import android.graphics.Bitmap;
import com.google.zxing.*;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import androidx.appcompat.app.AppCompatActivity;

public class QRCode extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle saveInstanceState){
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_qr_code);

        String eventID = "myapp://event?id=" + getIntent().getStringExtra("Event_ID");

        findViewById(R.id.qrCodeBack).setOnClickListener(v -> finish());

        ImageView qrCode = findViewById(R.id.qrCode);

        try{
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();

            Bitmap bitmap = barcodeEncoder.encodeBitmap(eventID, BarcodeFormat.QR_CODE, 400, 400);

            qrCode.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
