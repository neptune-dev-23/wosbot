package cl.camodev.utiles;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;

public class ImageSearchUtil {

	/**
	 * Busca el template dentro de la imagen principal en la región especificada
	 * (ROI). La imagen principal se espera que se encuentre en el directorio de
	 * ejecución en la carpeta "temp" con nombre "foto.png". La plantilla se carga
	 * desde los recursos del programa.
	 *
	 * @param roiX                 Coordenada X de inicio de la ROI.
	 * @param roiY                 Coordenada Y de inicio de la ROI.
	 * @param roiWidth             Ancho de la ROI.
	 * @param roiHeight            Alto de la ROI.
	 * @param templateResourcePath Ruta del recurso de la plantilla, por ejemplo:
	 *                             "/templates/template.png"
	 * @return Un objeto DTOImageSearchResult que indica si se encontró el template
	 *         (true si el valor máximo es mayor o igual a 0.90) y la posición (tipo
	 *         DTOPoint) donde se encontró.
	 */
	public static DTOImageSearchResult buscarTemplate(String templateResourcePath, int roiX, int roiY, int roiWidth,
			int roiHeight) {
		// Construir la ruta de la imagen principal basada en el directorio de
		// ejecución.
		String currentDir = System.getProperty("user.dir");
		String imagenPrincipalPath = currentDir + File.separator + "temp" + File.separator + "foto.png";
		Mat imagenPrincipal = Imgcodecs.imread(imagenPrincipalPath);
		if (imagenPrincipal.empty()) {
//			System.err.println("Error al cargar la imagen principal desde: " + imagenPrincipalPath);
			return new DTOImageSearchResult(false, null);
		}

		// Cargar la plantilla desde los recursos del programa
		InputStream is = ImageSearchUtil.class.getResourceAsStream(templateResourcePath);
		if (is == null) {
//			System.err.println("No se encontró el recurso del template: " + templateResourcePath);
			return new DTOImageSearchResult(false, null);
		}
		byte[] templateBytes;
		try {
			templateBytes = readAllBytes(is);
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
			return new DTOImageSearchResult(false, null);
		}

		// Convertir el arreglo de bytes a un Mat usando Imgcodecs.imdecode
		MatOfByte mob = new MatOfByte(templateBytes);
		Mat template = Imgcodecs.imdecode(mob, Imgcodecs.IMREAD_COLOR);
		if (template.empty()) {
//			System.err.println("Error al decodificar la plantilla desde el recurso: " + templateResourcePath);
			return new DTOImageSearchResult(false, null);
		}

		// Validar que la ROI esté dentro de la imagen principal
		if (roiX + roiWidth > imagenPrincipal.cols() || roiY + roiHeight > imagenPrincipal.rows()) {
//			System.err.println("La región definida se sale de los límites de la imagen principal.");
			return new DTOImageSearchResult(false, null);
		}

		// Crear la ROI en la imagen principal
		Rect roi = new Rect(roiX, roiY, roiWidth, roiHeight);
		Mat imagenROI = new Mat(imagenPrincipal, roi);

		// Verificar que la ROI sea lo suficientemente grande para el template
		int resultCols = imagenROI.cols() - template.cols() + 1;
		int resultRows = imagenROI.rows() - template.rows() + 1;
		if (resultCols <= 0 || resultRows <= 0) {
//			System.err.println("La plantilla es más grande que el área ROI especificada.");
			return new DTOImageSearchResult(false, null);
		}

		// Preparar la matriz donde se almacenará el resultado de la coincidencia
		Mat resultado = new Mat(resultRows, resultCols, CvType.CV_32FC1);

		// Realizar la coincidencia de plantilla usando TM_CCOEFF_NORMED
		Imgproc.matchTemplate(imagenROI, template, resultado, Imgproc.TM_CCOEFF_NORMED);

		// Buscar la mejor coincidencia (valor máximo y su ubicación)
		Core.MinMaxLocResult mmr = Core.minMaxLoc(resultado);
		double threshold = 0.9;
		if (mmr.maxVal < threshold) {
		    // Normaliza la matriz de resultado a un rango de 0 a 255 para poder visualizarla correctamente.
		    Mat debugResult = new Mat();
		    Core.normalize(resultado, debugResult, 0, 255, Core.NORM_MINMAX, CvType.CV_8U);

		    // Se obtiene la posición donde se encontró el valor máximo (en la imagen de resultado).
		    Point matchLoc = mmr.maxLoc;
		    
		    // Si tienes acceso a la variable 'template' (la imagen de plantilla), utiliza sus dimensiones.
		    // De lo contrario, puedes definir valores fijos o pasarlos como parámetros.
		    int templateWidth = template.cols();
		    int templateHeight = template.rows();
		    
		    // Se calcula el punto opuesto para el rectángulo.
		    Point rectEnd = new Point(matchLoc.x + templateWidth, matchLoc.y + templateHeight);
		    
		    // Dibuja un rectángulo en la imagen de depuración.
		    Imgproc.rectangle(debugResult, matchLoc, rectEnd, new Scalar(0, 255, 0), 2);

		    // Construir la ruta de salida basada en el directorio de ejecución actual.
		  
		    String debugFilePath = currentDir + File.separator + "temp" + File.separator + "debug_result.png";

		    // Guardar la imagen de depuración.
		    boolean saved = Imgcodecs.imwrite(debugFilePath, debugResult);
		    if (saved) {
		        System.out.println("Archivo de depuración guardado en: " + debugFilePath);
		    } else {
		        System.err.println("No se pudo guardar el archivo de depuración en: " + debugFilePath);
		    }

		    System.out.println("No se encontró una coincidencia lo suficientemente exacta. Valor: " + mmr.maxVal);
		    return new DTOImageSearchResult(false, null);
		}


		// Ajustar la ubicación de la coincidencia al sistema de coordenadas de la
		// imagen principal
		Point matchLoc = mmr.maxLoc;
		matchLoc.x += roi.x;
		matchLoc.y += roi.y;

		// Convertir la posición a un DTOPoint
		DTOPoint dtoPoint = new DTOPoint(matchLoc.x, matchLoc.y);

		// Retornar el resultado de la búsqueda
		return new DTOImageSearchResult(true, dtoPoint);
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
