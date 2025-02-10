package cl.camodev.utiles;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class UtilCV {

	public static void loadNativeLibrary(String resourcePath) throws IOException {
		// Obtener el nombre del archivo a partir de la ruta del recurso
		String[] parts = resourcePath.split("/");
		String libFileName = parts[parts.length - 1];

		// Crear un archivo temporal (se eliminará al salir de la aplicación)
		File tempLib = File.createTempFile(libFileName, "");
		tempLib.deleteOnExit();

		// Abrir el recurso como stream
		try (InputStream in = UtilCV.class.getResourceAsStream(resourcePath); OutputStream out = new FileOutputStream(tempLib)) {

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

	public static void extractResourceFolder(String resourcePath, File destination) throws IOException, URISyntaxException {
		// Crea el directorio de destino si no existe
		if (!destination.exists()) {
			if (!destination.mkdirs()) {
				throw new IOException("No se pudo crear el directorio: " + destination.getAbsolutePath());
			}
		}

		URL url = UtilCV.class.getResource(resourcePath);
		if (url == null) {
			throw new IOException("No se pudo encontrar la carpeta " + resourcePath + " en los recursos.");
		}

		// Si se ejecuta desde un JAR, el protocolo será "jar"
		if (url.getProtocol().equals("jar")) {
			// Se obtiene la ruta del JAR
			String jarPath = url.getPath().substring(5, url.getPath().indexOf("!"));
			try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"))) {
				// Se remueve la barra inicial si existe para trabajar con las entradas
				String resourcePathNoSlash = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
				Enumeration<JarEntry> entries = jar.entries();
				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					String entryName = entry.getName();
					// Se comprueba si la entrada pertenece a la carpeta de recursos deseada
					if (entryName.startsWith(resourcePathNoSlash + "/")) {
						// Se calcula la ruta relativa dentro de la carpeta
						String relativePath = entryName.substring(resourcePathNoSlash.length() + 1);
						File outFile = new File(destination, relativePath);
						if (entry.isDirectory()) {
							outFile.mkdirs();
						} else {
							// Asegurarse de que el directorio padre exista
							outFile.getParentFile().mkdirs();
							try (InputStream in = jar.getInputStream(entry); OutputStream out = new FileOutputStream(outFile)) {
								byte[] buffer = new byte[4096];
								int bytesRead;
								while ((bytesRead = in.read(buffer)) != -1) {
									out.write(buffer, 0, bytesRead);
								}
							}
						}
					}
				}
			}
		} else if (url.getProtocol().equals("file")) {
			// Si se ejecuta desde el sistema de archivos (por ejemplo, en el IDE)
			File folder = new File(url.toURI());
			copyDirectory(folder, destination);
		} else {
			throw new IOException("Protocolo no soportado: " + url.getProtocol());
		}
	}

	/**
	 * Copia recursivamente el contenido de un directorio a otro.
	 *
	 * @param source      Directorio de origen
	 * @param destination Directorio de destino
	 * @throws IOException
	 */
	private static void copyDirectory(File source, File destination) throws IOException {
		if (source.isDirectory()) {
			if (!destination.exists()) {
				destination.mkdirs();
			}
			String[] children = source.list();
			if (children != null) {
				for (String child : children) {
					copyDirectory(new File(source, child), new File(destination, child));
				}
			}
		} else {
			Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

}
