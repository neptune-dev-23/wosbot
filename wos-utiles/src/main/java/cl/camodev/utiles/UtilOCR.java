package cl.camodev.utiles;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import cl.camodev.wosbot.ot.DTOPoint;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public class UtilOCR {

    /**
     * Performs OCR on a specified region of a BufferedImage using Tesseract.
     * 
     * @param image    Buffered image to process.
     * @param p1       Top-left point that defines the region.
     * @param p2       Bottom-right point that defines the region.
     * @param language Language code for Tesseract (e.g., "eng" for English, "spa"
     *                 for Spanish).
     * @return Extracted text from the specified region.
     * @throws TesseractException       If an error occurs during OCR processing.
     * @throws IllegalArgumentException If the image is null or the specified region
     *                                  is invalid.
     */
    public static String ocrFromRegion(BufferedImage image, DTOPoint p1, DTOPoint p2, String language)
            throws TesseractException {
        if (image == null) {
            throw new IllegalArgumentException("Image cannot be null.");
        }

        int x = (int) Math.min(p1.getX(), p2.getX());
        int y = (int) Math.min(p1.getY(), p2.getY());
        int width = (int) Math.abs(p1.getX() - p2.getX());
        int height = (int) Math.abs(p1.getY() - p2.getY());

        if (x + width > image.getWidth() || y + height > image.getHeight()) {
            throw new IllegalArgumentException("Specified region exceeds image bounds.");
        }

        BufferedImage subImage = image.getSubimage(x, y, width, height);

        // Upscale x2 for clarity
        BufferedImage resizedImage = new BufferedImage(width * 2, height * 2, subImage.getType());
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(subImage, 0, 0, width * 2, height * 2, null);
        g2d.dispose();

        // Optional: dump debug images to /tmp
        // try {
        // ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // ImageIO.write(resizedImage, "png", baos);
        // java.nio.file.Files.write(
        // java.nio.file.Files.createTempFile("img_cut_resized-", ".png"),
        // baos.toByteArray()
        // );
        // } catch (IOException e) {
        // e.printStackTrace();
        // }

        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("lib/tesseract");
        tesseract.setLanguage(language);

        // Configurations to improve numeric OCR
        tesseract.setPageSegMode(7); // single line
        tesseract.setOcrEngineMode(1); // LSTM only

        return tesseract.doOCR(resizedImage).replace("\n", "").replace("\r", "").trim();
    }
}
