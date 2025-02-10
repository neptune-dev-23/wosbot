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

public class ImageSearchUtil {

	/**
	 * Realiza la búsqueda de un template (plantilla) dentro de una imagen principal.
	 * <p>
	 * La imagen principal se carga desde una ruta externa, mientras que el template se obtiene de los recursos del jar. Se define una región de
	 * interés (ROI) en la imagen principal para limitar la búsqueda. La coincidencia se realiza utilizando el método TM_CCOEFF_NORMED de
	 * OpenCV. El porcentaje de coincidencia se obtiene multiplicando el valor máximo de la coincidencia por 100, y se compara con el umbral
	 * proporcionado.
	 * </p>
	 *
	 * @param imagenPrincipalPath  Ruta de la imagen externa a comparar.
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
	public static DTOImageSearchResult buscarTemplate(String imagenPrincipalPath, String templateResourcePath, int roiX, int roiY, int roiWidth, int roiHeight, double thresholdPercentage) {

		// Cargar la imagen principal (externa) desde la ruta proporcionada.
		Mat imagenPrincipal = Imgcodecs.imread(imagenPrincipalPath);
		if (imagenPrincipal.empty()) {
			System.err.println("Error al cargar la imagen principal desde: " + imagenPrincipalPath);
			return new DTOImageSearchResult(false, null, 0.0);
		}

		// Cargar la plantilla desde los recursos del programa.
		InputStream is = ImageSearchUtil.class.getResourceAsStream(templateResourcePath);
		if (is == null) {
			System.err.println("No se encontró el recurso del template: " + templateResourcePath);
			return new DTOImageSearchResult(false, null, 0.0);
		}
		byte[] templateBytes;
		try {
			templateBytes = readAllBytes(is);
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
			return new DTOImageSearchResult(false, null, 0.0);
		}

		// Convertir el arreglo de bytes a un Mat utilizando Imgcodecs.imdecode.
		MatOfByte mob = new MatOfByte(templateBytes);
		Mat template = Imgcodecs.imdecode(mob, Imgcodecs.IMREAD_COLOR);
		if (template.empty()) {
			System.err.println("Error al decodificar la plantilla desde el recurso: " + templateResourcePath);
			return new DTOImageSearchResult(false, null, 0.0);
		}

		// Validar que la ROI esté dentro de la imagen principal.
		if (roiX + roiWidth > imagenPrincipal.cols() || roiY + roiHeight > imagenPrincipal.rows()) {
			System.err.println("La región definida se sale de los límites de la imagen principal.");
			return new DTOImageSearchResult(false, null, 0.0);
		}

		// Crear la ROI en la imagen principal.
		Rect roi = new Rect(roiX, roiY, roiWidth, roiHeight);
		Mat imagenROI = new Mat(imagenPrincipal, roi);

		// Verificar que la ROI sea lo suficientemente grande para el template.
		int resultCols = imagenROI.cols() - template.cols() + 1;
		int resultRows = imagenROI.rows() - template.rows() + 1;
		if (resultCols <= 0 || resultRows <= 0) {
			System.err.println("La plantilla es más grande que el área ROI especificada.");
			return new DTOImageSearchResult(false, null, 0.0);
		}

		// Preparar la matriz donde se almacenará el resultado de la coincidencia.
		Mat resultado = new Mat(resultRows, resultCols, CvType.CV_32FC1);

		// Realizar la coincidencia de plantilla usando TM_CCOEFF_NORMED.
		Imgproc.matchTemplate(imagenROI, template, resultado, Imgproc.TM_CCOEFF_NORMED);

		// Buscar la mejor coincidencia (valor máximo y su ubicación).
		Core.MinMaxLocResult mmr = Core.minMaxLoc(resultado);
		double matchPercentage = mmr.maxVal * 100.0; // Convertir el valor de coincidencia a porcentaje.

		// Comparar usando el umbral dado en porcentaje.
		if (matchPercentage < thresholdPercentage) {
			System.out.println("No se encontró una coincidencia lo suficientemente exacta. Valor: " + matchPercentage);
			return new DTOImageSearchResult(false, null, matchPercentage);
		}

		// Ajustar la ubicación de la coincidencia al sistema de coordenadas de la imagen principal.
		Point matchLoc = mmr.maxLoc;
		matchLoc.x += roi.x;
		matchLoc.y += roi.y;

		// Convertir la posición a un DTOPoint.
		DTOPoint dtoPoint = new DTOPoint(matchLoc.x, matchLoc.y);

		// Retornar el resultado de la búsqueda, incluyendo el porcentaje de coincidencia.
		return new DTOImageSearchResult(true, dtoPoint, matchPercentage);
	}

	/**
	 * Lee todos los bytes de un InputStream.
	 *
	 * @param is InputStream a leer.
	 * @return Un arreglo de bytes con el contenido leído.
	 * @throws IOException Si ocurre algún error durante la lectura.
	 */
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
