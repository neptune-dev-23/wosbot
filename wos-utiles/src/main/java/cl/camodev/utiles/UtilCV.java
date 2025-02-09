package cl.camodev.utiles;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class UtilCV {

	public static void loadNativeLibrary(String resourcePath) throws IOException {
		// Obtener el nombre del archivo a partir de la ruta del recurso
		String[] parts = resourcePath.split("/");
		String libFileName = parts[parts.length - 1];

		// Crear un archivo temporal (se eliminará al salir de la aplicación)
		File tempLib = File.createTempFile(libFileName, "");
		tempLib.deleteOnExit();

		// Abrir el recurso como stream
		try (InputStream in = UtilCV.class.getResourceAsStream(resourcePath);
				OutputStream out = new FileOutputStream(tempLib)) {

			if (in == null) {
				throw new IOException("No se encontró el recurso: " + resourcePath);
			}

			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, bytesRead);
			}
		}

		// Cargar la librería usando la ruta absoluta del archivo temporal
		System.load(tempLib.getAbsolutePath());
		System.out.println("Librería cargada desde: " + tempLib.getAbsolutePath());
	}

}
