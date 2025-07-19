package cl.camodev.utiles;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import org.slf4j.*;

public class ImageSearchUtil {
	private static final Logger logger = LoggerFactory.getLogger(ImageSearchUtil.class);

	/**
	 * Realiza la búsqueda de un template (plantilla) dentro de una imagen principal.
	 * <p>
	 * La imagen principal se carga desde una ruta externa, mientras que el template se obtiene de los recursos del jar. Se define una región de
	 * interés (ROI) en la imagen principal para limitar la búsqueda. La coincidencia se realiza utilizando el método TM_CCOEFF_NORMED de
	 * OpenCV. El porcentaje de coincidencia se obtiene multiplicando el valor máximo de la coincidencia por 100, y se compara con el umbral
	 * proporcionado.
	 * </p>
	 *
	 * @param templateResourcePath Ruta del template dentro de los recursos del jar.
	 * @param roiX                 Coordenada X del punto superior izquierdo de la ROI.
	 * @param roiY                 Coordenada Y del punto superior izquierdo de la ROI.
	 * @param roiWidth             Ancho de la ROI.
	 * @param roiHeight            Alto de la ROI.
	 * @param thresholdPercentage  Umbral de coincidencia en porcentaje (0 a 100). Si el porcentaje de coincidencia es menor que este valor, se
	 *                             considerará que no hay coincidencia suficiente.
	 * @return Un objeto {@link DTOImageSearchResult} que contiene:
	 *         <ul>
	 *         <li>El estado de la búsqueda (true si se encontró una coincidencia adecuada, false en caso contrario).</li>
	 *         <li>La posición de la coincidencia (como {@link DTOPoint}) en la imagen principal, ajustada al sistema de coordenadas de la
	 *         misma.</li>
	 *         <li>El porcentaje de coincidencia obtenido.</li>
	 *         </ul>
	 */

	public static DTOImageSearchResult buscarTemplate(byte[] image, String templateResourcePath, int roiX, int roiY, int roiWidth, int roiHeight, double thresholdPercentage) {
		try {

			// Decodificar la imagen principal directamente desde el byte[]
			MatOfByte matOfByte = new MatOfByte(image);
			Mat imagenPrincipal = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR);

			if (imagenPrincipal.empty()) {
				logger.error("Error while loading image from byte array.");
				return new DTOImageSearchResult(false, null, 0.0);
			}

			// Cargar la plantilla desde los recursos
			InputStream is = ImageSearchUtil.class.getResourceAsStream(templateResourcePath);
			if (is == null) {
				logger.error(templateResourcePath + " not found.");
				return new DTOImageSearchResult(false, null, 0.0);
			}

			// Leer bytes del template
			byte[] templateBytes = is.readAllBytes();
			is.close();

			// Decodificar el template en un Mat
			MatOfByte templateMatOfByte = new MatOfByte(templateBytes);
			Mat template = Imgcodecs.imdecode(templateMatOfByte, Imgcodecs.IMREAD_COLOR);

			if (template.empty()) {
				logger.error("Error decoding template.");
				return new DTOImageSearchResult(false, null, 0.0);
			}

			// Validar la ROI
			if (roiX + roiWidth > imagenPrincipal.cols() || roiY + roiHeight > imagenPrincipal.rows()) {
				logger.error("Defined ROI is out of bounds.");
				return new DTOImageSearchResult(false, null, 0.0);
			}

			// Crear la ROI
			Rect roi = new Rect(roiX, roiY, roiWidth, roiHeight);
			Mat imagenROI = new Mat(imagenPrincipal, roi);

			// Verificar tamaño
			int resultCols = imagenROI.cols() - template.cols() + 1;
			int resultRows = imagenROI.rows() - template.rows() + 1;
			if (resultCols <= 0 || resultRows <= 0) {
				logger.error("Template is larger than ROI.");
				return new DTOImageSearchResult(false, null, 0.0);
			}

			// Coincidencia de plantilla
			Mat resultado = new Mat(resultRows, resultCols, CvType.CV_32FC1);
			Imgproc.matchTemplate(imagenROI, template, resultado, Imgproc.TM_CCOEFF_NORMED);

			// Obtener el mejor match
			Core.MinMaxLocResult mmr = Core.minMaxLoc(resultado);
			double matchPercentage = mmr.maxVal * 100.0;

			if (matchPercentage < thresholdPercentage) {
				logger.info("Template " + templateResourcePath + " not found. Match percentage: " + matchPercentage);
				return new DTOImageSearchResult(false, null, matchPercentage);
			}

			// Ajustar la coordenada para que esté en el centro de la coincidencia
			Point matchLoc = mmr.maxLoc;
			double centerX = matchLoc.x + roi.x + (template.cols() / 2.0);
			double centerY = matchLoc.y + roi.y + (template.rows() / 2.0);

			return new DTOImageSearchResult(true, new DTOPoint((int) centerX, (int) centerY), matchPercentage);

		} catch (IOException e) {
			logger.error("Exception during template search.", e);
			return new DTOImageSearchResult(false, null, 0.0);
		}
	}

	private static byte[] readAllBytes(InputStream is) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int nRead;
		byte[] data = new byte[16384];
		while ((nRead = is.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}
		buffer.flush();
		return buffer.toByteArray();
	}
}
