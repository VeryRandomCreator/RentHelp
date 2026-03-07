package com.veryrandomcreator.tenanthelp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfDocument.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

// Code developed manually through android documentation
public class PropertyPackage {
    public static final int PDF_PADDING = 10;
    public static final int PDF_PAGE_HEIGHT = 842;
    public static final int PDF_PAGE_WIDTH = 595;
    public static final Rect MAX_IMAGE_DIM = new Rect(0, 0, PDF_PAGE_WIDTH, PDF_PAGE_HEIGHT / 2);

    private List<PropertyImage> images = new ArrayList<>();
    private String latestHash;

    public void setImages(List<PropertyImage> images) {
        this.images = images;
    }

    public List<PropertyImage> getImages() {
        return images;
    }

    public String getLatestHash() {
        return latestHash;
    }

    public void fetchLatestHash() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://mempool.space/api/blocks/tip/hash");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String hash = in.readLine();
                    in.close();

                    latestHash = hash;
                    System.out.println(latestHash);
                } catch (MalformedURLException | ProtocolException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }

            }
        });
        thread.start();
    }

    // TODO: ADD CHECK FOR LATESTHASH
    private Bitmap getStampedImage(Context context, PropertyImage image) throws GeneralSecurityException, IOException {
        Bitmap bitmap = PhotoStorageManager.loadPhotoBitmap(context, image.getId(), true);

        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setTextSize(50f);
        paint.setShadowLayer(5f, 2f, 2f, Color.BLACK);

        canvas.drawText(latestHash, canvas.getWidth() / 2, canvas.getHeight() / 2, paint);

        return bitmap;
    }

    // generates a pdf such that two there are two images per page
    public PdfDocument generatePdf(Context context) throws GeneralSecurityException, IOException {
        PdfDocument document = new PdfDocument();
        PageInfo pageInfo = null;
        Page page = null;
        Canvas canvas = null;

        Bitmap stampedImage;

        boolean hasFinishedPage = false;
        for (int i = 0; i < images.size(); i++) {
            System.out.println("WRITING");
            if (i % 2 == 0) {
                pageInfo = new PageInfo.Builder(PDF_PAGE_WIDTH, PDF_PAGE_HEIGHT, i / 2 + 1).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                hasFinishedPage = false;
            }

            stampedImage = getStampedImage(context, images.get(i));
            Rect dest = new Rect(0, 0, stampedImage.getWidth(), stampedImage.getHeight());
            scaleToFit(MAX_IMAGE_DIM, dest);
            dest.top += i % 2 * MAX_IMAGE_DIM.bottom;
            dest.bottom += i % 2 * MAX_IMAGE_DIM.bottom;
            System.out.println("BEFORE BITMAP: " + stampedImage);
            canvas.drawBitmap(stampedImage, null, dest, null);
            System.out.println("AFTER BITMAP");

            // also draw text and other info

            if (i % 2 == 1) {
                document.finishPage(page);
                hasFinishedPage = true;
            }
            System.out.println("after FINISH");
        }
        if (!hasFinishedPage) {
            document.finishPage(page);
        }
        return document;
    }

    // This method scales dest to fit in parent while maintaining aspect ratio of dest
    private void scaleToFit(final Rect parent, Rect dest) {
        float scaleX = parent.right / (float) dest.right;
        float scaleY = parent.bottom / (float) dest.bottom;
        float scale = Math.min(scaleX, scaleY);
        dest.right = (int) (dest.right * scale);
        dest.bottom = (int) (dest.bottom * scale);

    }
}
