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
    private String label;
    private String description;

    public void setImages(List<PropertyImage> images) {
        this.images = images;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<PropertyImage> getImages() {
        return images;
    }

    // Assumes latesthash has been set
    private Bitmap getStampedImage(Context context, PropertyImage image) throws GeneralSecurityException, IOException {
        Bitmap bitmap = PhotoStorageManager.loadPhotoBitmap(context, image.getId(), true);

        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setTextSize(50f);
        paint.setShadowLayer(5f, 2f, 2f, Color.BLACK);

        canvas.drawText(image.getLatestHash(), 0, 50, paint);

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
            if (i % 2 == 0) {
                pageInfo = new PageInfo.Builder(PDF_PAGE_WIDTH, PDF_PAGE_HEIGHT, i / 2 + 1).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                hasFinishedPage = false;
            }

            stampedImage = getStampedImage(context, images.get(i));
            Rect dest = new Rect(PDF_PADDING, PDF_PADDING, stampedImage.getWidth() - PDF_PADDING, stampedImage.getHeight() - PDF_PADDING);
            scaleToFit(MAX_IMAGE_DIM, dest);
            dest.top += i % 2 * MAX_IMAGE_DIM.bottom;
            dest.bottom += i % 2 * MAX_IMAGE_DIM.bottom;
            canvas.drawBitmap(stampedImage, null, dest, null);

            // also draw text and other info

            if (i % 2 == 1) {
                document.finishPage(page);
                hasFinishedPage = true;
            }
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
