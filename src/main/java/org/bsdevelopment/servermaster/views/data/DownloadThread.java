package org.bsdevelopment.servermaster.views.data;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.progressbar.ProgressBar;

import java.io.*;
import java.net.HttpURLConnection;

public class DownloadThread extends Thread {
    public DownloadThread(File file, HttpURLConnection httpConnection, UI ui, ProgressBar progressBar, Runnable finishTask) {
        super(() -> {
            try {
                long completeFileSize = httpConnection.getContentLength();

                BufferedInputStream inputStream = new BufferedInputStream(httpConnection.getInputStream());
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                BufferedOutputStream outputStream = new BufferedOutputStream(fileOutputStream, 1024);

                byte[] data = new byte[1024];
                long downloadedFileSize = 0;
                int dataLength = 0;
                while ((!DownloadThread.currentThread().isInterrupted()) && (dataLength = inputStream.read(data, 0, 1024)) != -1) {
                    downloadedFileSize += dataLength;

                    int currentProgress = (int) (downloadedFileSize * 100 / completeFileSize);
                    if (currentProgress >= 100) break;

                    ui.access(() -> {
                        progressBar.setValue(currentProgress);
                    });
                    outputStream.write(data, 0, dataLength);
                }

                ui.access(finishTask::run);
                outputStream.close();
                inputStream.close();

                // DownloadThread.currentThread().interrupt();
            } catch (IOException ignored) {}
        });
    }
}
