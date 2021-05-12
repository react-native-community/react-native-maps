package com.airbnb.android.react.maps;

import android.content.Context;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;
 
public class AirMapUrlTileWorker extends Worker {
	private static final int BUFFER_SIZE = 16 * 1024;

	public AirMapUrlTileWorker(
			@NonNull Context context,
			@NonNull WorkerParameters params) {
			super(context, params);
	}

	@Override
	public Result doWork() {
		byte[] image = null;
		URL url = null;

		Log.d("tileCachePath: ", "Worker");
		try {
      url = new URL(getInputData().getString("url"));
    } catch (MalformedURLException e) {
      throw new AssertionError(e);
    }
		
		String fileName = getInputData().getString("filename");
		Log.d("tileCachePath: ", url.toString());      

		image = fetchTile(url);
		if (image != null) {
			boolean success = writeTileImage(image, fileName);
			if (!success) {
				Log.d("tileCachePath:", "Worker saving to cache failed");
				return Result.failure();
			}
    } else {
			Log.d("tileCachePath: ", "Worker fetching tile failed");
			return Result.retry();
		}

		// Indicate whether the work finished successfully with the Result
		return Result.success();
	}

	private byte[] fetchTile(URL url) {
      ByteArrayOutputStream buffer = null;
      InputStream in = null;

      //Log.d("tileCachePath: fetchTile: ", '/' + Integer.toString(zoom) + 
      //  "/" + Integer.toString(x) + "/" + Integer.toString(y));

      try {
        in = url.openStream();
        buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[BUFFER_SIZE];

        while ((nRead = in.read(data, 0, BUFFER_SIZE)) != -1) {
          buffer.write(data, 0, nRead);
        }
        buffer.flush();

        return buffer.toByteArray();
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      } catch (OutOfMemoryError e) {
        e.printStackTrace();
        return null;
      } finally {
        if (in != null) try { in.close(); } catch (Exception ignored) {}
        if (buffer != null) try { buffer.close(); } catch (Exception ignored) {}
      }
    }

	private boolean writeTileImage(byte[] image, String fileName) {
      OutputStream out = null;
      if (fileName == null) {
        return false;
      }

      try {
        File file = new File(fileName);
        file.getParentFile().mkdirs();
        out = new FileOutputStream(file);
        out.write(image);

        return true;
      } catch (IOException e) {
        e.printStackTrace();
        return false;
      } catch (OutOfMemoryError e) {
        e.printStackTrace();
        return false;
      } finally {
        if (out != null) try { out.close(); } catch (Exception ignored) {}
      }
  }
}
