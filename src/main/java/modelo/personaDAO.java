package modelo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JTable;

public class personaDAO {

    // Ruta donde se almacenan los contactos
    private final Path archivo;

    // Encabezado del archivo CSV
    private static final String ENCABEZADO
            = "NOMBRE;TELEFONO;EMAIL;CATEGORIA;FAVORITO";

    // Constructor
    public personaDAO() throws IOException {

        Path carpeta = Paths.get(
                System.getProperty("user.home"),
                "gestionContactos"
        );

        Files.createDirectories(carpeta);

        archivo = carpeta.resolve("datosContactos.csv");

        prepararArchivo();
    }

    // Crear el archivo CSV si no existe
    private void prepararArchivo() throws IOException {

        if (Files.notExists(archivo)) {

            try (BufferedWriter escritor
                    = Files.newBufferedWriter(
                            archivo,
                            StandardCharsets.UTF_8)) {

                escritor.write(ENCABEZADO);
                escritor.newLine();
            }
        }
    }

    // Agregar un contacto al archivo
    public boolean escribirArchivo(persona contacto)
            throws IOException {

        validarContacto(contacto);

        try (BufferedWriter escritor
                = Files.newBufferedWriter(
                        archivo,
                        StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.APPEND)) {

            escritor.write(contacto.datosContacto());
            escritor.newLine();
        }

        return true;
    }

    // Leer todos los contactos registrados
    public List<persona> leerArchivo() throws IOException {

        List<persona> personas = new ArrayList<>();

        try (BufferedReader lector
                = Files.newBufferedReader(
                        archivo,
                        StandardCharsets.UTF_8)) {

            String linea;

            // Omitir encabezado
            lector.readLine();

            while ((linea = lector.readLine()) != null) {

                if (linea.trim().isEmpty()) {
                    continue;
                }

                String[] datos = linea.split(";", -1);

                if (datos.length != 5) {
                    throw new IOException(
                            "Registro CSV incorrecto: " + linea);
                }

                persona contacto = new persona(
                        datos[0],
                        datos[1],
                        datos[2],
                        datos[3],
                        Boolean.parseBoolean(datos[4])
                );

                personas.add(contacto);
            }
        }

        return personas;
    }

    // Actualizar todos los contactos del archivo
    public void actualizarContactos(List<persona> personas)
            throws IOException {

        Path temporal = Files.createTempFile(
                archivo.getParent(),
                "contactos_",
                ".tmp"
        );

        try {

            try (BufferedWriter escritor
                    = Files.newBufferedWriter(
                            temporal,
                            StandardCharsets.UTF_8)) {

                escritor.write(ENCABEZADO);
                escritor.newLine();

                for (persona contacto : personas) {

                    validarContacto(contacto);

                    escritor.write(contacto.datosContacto());
                    escritor.newLine();
                }
            }

            // Reemplazar el archivo original
            Files.move(
                    temporal,
                    archivo,
                    StandardCopyOption.REPLACE_EXISTING
            );

        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    // Validar datos antes de almacenarlos
    private void validarContacto(persona contacto)
            throws IOException {

        if (contacto == null) {
            throw new IOException("El contacto es nulo.");
        }

        String[] campos = {
            contacto.getNombre(),
            contacto.getTelefono(),
            contacto.getEmail(),
            contacto.getCategoria()
        };

        for (String campo : campos) {

            if (campo == null
                    || campo.contains(";")
                    || campo.contains("\n")
                    || campo.contains("\r")) {

                throw new IOException(
                        "El contacto contiene datos no válidos."
                );
            }
        }
    }

    // EXPORTAR CONTACTOS A CSV
    public void exportarCSV(File destino) throws IOException {

        List<persona> personas = leerArchivo();

        try (BufferedWriter escritor = Files.newBufferedWriter(
                destino.toPath(), StandardCharsets.UTF_8)) {

            escritor.write(ENCABEZADO);
            escritor.newLine();

            for (persona contacto : personas) {
                escritor.write(contacto.datosContacto());
                escritor.newLine();
            }
        }
    }
}
