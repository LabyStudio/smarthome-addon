package de.labystudio.desktopmodules.smarthome.api.mjpeg;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

/**
 * Motion picture stream decoder
 *
 * @author LabyStudio
 */
public class MotionPictureStream {

    private static final int BUFFER_SIZE = 8192;

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);

    private final URL url;

    private InputStream stream;
    private ByteArrayOutputStream buffer;
    private BufferedImage currentFrame;

    private boolean alive = false;
    private boolean loading = true;

    private boolean isInsideOfFrame = false;

    private final List<Consumer<BufferedImage>> frameListeners = new ArrayList<>();

    /**
     * A motion picture stream reader (MJPEG decoder)
     *
     * @param url The remote url to read an decode
     */
    public MotionPictureStream(URL url) {
        this.url = url;
    }

    /**
     * Open the stream and start reading frames asynchronously
     */
    public void openAsync() {
        this.executorService.execute(() -> {
            try {
                open();
            } catch (SocketException e) {
                // Ignore socket closed exception
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Open the stream and start reading frames
     *
     * @throws IOException Read exception
     */
    private void open() throws IOException {
        this.alive = true;
        this.loading = true;

        // Open stream
        this.stream = new BufferedInputStream(this.url.openStream(), BUFFER_SIZE);

        int previousByte = 0;
        int currentByte;

        // Read jpeg images
        while (this.alive && (currentByte = this.stream.read()) >= 0) {
            // Read next packet frame
            BufferedImage frame = read(previousByte, currentByte);

            if (frame != null) {
                // Frame is completed
                this.loading = false;
                this.currentFrame = frame;

                // Call listeners
                for (Consumer<BufferedImage> listener : this.frameListeners) {
                    listener.accept(frame);
                }
            }

            previousByte = currentByte;
        }

        // Close stream
        close();
    }

    /**
     * Read a packet frame and return a video frame when the packet is fully received
     *
     * @param previousByte The previous byte of the video transmission
     * @param currentByte  The current byte of the video transmission
     * @return The fully received packet as picture
     * @throws IOException Buffer write exception
     */
    private BufferedImage read(int previousByte, int currentByte) throws IOException {
        boolean isStartOfFrame = previousByte == 0xFF && currentByte == 0xD8;
        boolean isEndOfFrame = previousByte == 0xFF && currentByte == 0xD9;

        // Frame start
        if (isStartOfFrame) {
            this.isInsideOfFrame = true;

            // Close buffer
            if (this.buffer != null) {
                this.buffer.close();
            }

            // New buffer
            this.buffer = new ByteArrayOutputStream(BUFFER_SIZE);
            this.buffer.write((byte) previousByte);
        }

        // Write frame
        if (this.isInsideOfFrame) {
            this.buffer.write((byte) currentByte);

            // Frame end
            if (isEndOfFrame) {
                this.isInsideOfFrame = false;

                // Return full image
                return toBufferedImage(buffer);
            }
        }

        // No image in this step
        return null;
    }

    /**
     * Close the stream
     */
    public void close() throws IOException {
        this.alive = false;
        this.stream.close();
        this.buffer.close();
    }

    /**
     * Register a frame listener that will be called when receiving a full image frame
     *
     * @param listener The frame listener
     */
    public void registerListener(Consumer<BufferedImage> listener) {
        this.frameListeners.add(listener);
    }

    /**
     * Connection alive state
     *
     * @return Connection is alive
     */
    public boolean isAlive() {
        return alive;
    }

    /**
     * Indicates if an image has already been received
     *
     * @return Loading state
     */
    public boolean isLoading() {
        return loading;
    }

    /**
     * Get the last fully received frame
     *
     * @return Buffered image object
     */
    public BufferedImage getCurrentFrame() {
        return currentFrame;
    }

    /**
     * Convert byte array input to buffered image
     *
     * @param buffer The byte array
     * @return Buffered image from the byte array
     * @throws IOException Read exception
     */
    private BufferedImage toBufferedImage(ByteArrayOutputStream buffer) throws IOException {
        ByteArrayInputStream imageBuffer = new ByteArrayInputStream(buffer.toByteArray());
        BufferedImage bufferedImage = ImageIO.read(imageBuffer);
        imageBuffer.close();
        return bufferedImage;
    }

}
