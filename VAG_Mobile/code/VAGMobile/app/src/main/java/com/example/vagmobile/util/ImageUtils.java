package com.example.vagmobile.util;

import android.content.Context;
import android.net.Uri;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class ImageUtils {

    public static MultipartBody.Part prepareImagePart(String partName, File imageFile) {
        try {
            RequestBody requestFile = RequestBody.create(MultipartBody.FORM, imageFile);
            return MultipartBody.Part.createFormData(partName, imageFile.getName(), requestFile);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String convertCategoryIdsToString(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < categoryIds.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(categoryIds.get(i));
        }
        return sb.toString();
    }
    public static File uriToFile(Uri uri, Context context) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return null;
            }

            File file = new File(context.getCacheDir(), "temp_image_" + System.currentTimeMillis() + ".jpg");
            OutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[4 * 1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();

            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static MultipartBody.Part prepareImagePartFromUri(String partName, Uri imageUri, Context context) {
        try {
            File file = uriToFile(imageUri, context);
            if (file != null) {
                RequestBody requestFile = RequestBody.create(MultipartBody.FORM, file);
                return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}