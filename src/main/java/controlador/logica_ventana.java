package controlador;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import modelo.persona;
import modelo.personaDAO;
import vista.ventana;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.InputMap;
import javax.swing.ActionMap;
import javax.swing.SwingWorker;
import javax.swing.JProgressBar;
import java.util.concurrent.ExecutionException;

public class logica_ventana implements ActionListener,
        ListSelectionListener {

    // Referencia a la interfaz gráfica
    private final ventana delegado;

    // Acceso a los datos
    private personaDAO dao;

    // Lista de contactos registrados
    private List<persona> contactos = new ArrayList<>();

    // Modelo de la tabla
    private DefaultTableModel modeloTabla;

    // Ordenamiento y filtrado
    private TableRowSorter<DefaultTableModel> ordenador;

    // Constructor del controlador
    public logica_ventana(ventana delegado) {

        this.delegado = delegado;

        // Preparar tabla
        configurarTabla();

        // Registrar eventos de botones
        delegado.getBtnAgregar().addActionListener(this);
        delegado.getBtnModificar().addActionListener(this);
        delegado.getBtnEliminar().addActionListener(this);
        delegado.getBtnExportar().addActionListener(this);

        // Evento de selección de contactos
        delegado.getTblContactos()
                .getSelectionModel()
                .addListSelectionListener(this);

// Configurar menú contextual del mouse
        configurarMenuContextual();

// Configurar búsqueda
        configurarBusqueda();

// Configurar atajos de teclado
        configurarAtajosTeclado();
        // Inicializar acceso a datos y cargar contactos
        try {
            dao = new personaDAO();
            cargarContactosRegistrados();

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    delegado,
                    "Error al inicializar los contactos: "
                    + ex.getMessage()
            );
        }
    }

    // CONFIGURACIÓN DE LA TABLA
    private void configurarTabla() {

        String[] columnas = {
            "Nombre",
            "Teléfono",
            "Email",
            "Categoría",
            "Favorito"
        };

        modeloTabla = new DefaultTableModel(columnas, 0) {

            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columna) {
                return columna == 4
                        ? Boolean.class
                        : String.class;
            }
        };

        delegado.getTblContactos().setModel(modeloTabla);

        ordenador = new TableRowSorter<>(modeloTabla);
        delegado.getTblContactos().setRowSorter(ordenador);
    }

    // CONFIGURAR MENÚ CONTEXTUAL CON CLIC DERECHO
    private void configurarMenuContextual() {

        // Crear menú emergente
        JPopupMenu menu = new JPopupMenu();

        // Crear las opciones
        JMenuItem itemEditar = new JMenuItem("Editar contacto");
        JMenuItem itemEliminar = new JMenuItem("Eliminar contacto");

        // Agregar opciones al menú
        menu.add(itemEditar);
        menu.addSeparator();
        menu.add(itemEliminar);

        // Evento para editar un contacto
        itemEditar.addActionListener(e -> {

            int indice = obtenerIndiceSeleccionado();

            if (indice != -1) {

                // Los datos ya se cargan al seleccionar la fila.
                // Dar el foco al campo de nombres.
                delegado.getTxtNombres().requestFocusInWindow();

                delegado.getTxtNombres().selectAll();
            }
        });

        // Evento para eliminar un contacto
        itemEliminar.addActionListener(e -> {
            eliminarContacto();
        });

        // Obtener referencia a la tabla
        JTable tabla = delegado.getTblContactos();

        // Registrar evento de mouse
        tabla.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                mostrarMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mostrarMenu(e);
            }

            private void mostrarMenu(MouseEvent e) {

                // Comprobar si es un clic contextual
                if (e.isPopupTrigger()) {

                    // Identificar la fila bajo el cursor
                    int fila = tabla.rowAtPoint(e.getPoint());

                    if (fila >= 0) {

                        // Seleccionar la fila correspondiente
                        tabla.setRowSelectionInterval(fila, fila);

                        // Mostrar el menú en la posición del mouse
                        menu.show(
                                tabla,
                                e.getX(),
                                e.getY()
                        );
                    }
                }
            }
        });
    }

    // CONFIGURAR ATAJOS DE TECLADO
    private void configurarAtajosTeclado() {

        // Obtener los mapas de acciones de la ventana
        InputMap mapaTeclas = delegado.getRootPane()
                .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        ActionMap mapaAcciones = delegado.getRootPane()
                .getActionMap();

        // CTRL + N: NUEVO CONTACTO
        mapaTeclas.put(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_N,
                        InputEvent.CTRL_DOWN_MASK
                ),
                "nuevoContacto"
        );

        mapaAcciones.put("nuevoContacto", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                limpiarCampos();
                delegado.getTxtNombres().requestFocusInWindow();
            }
        });

        // CTRL + E: EXPORTAR CSV
        mapaTeclas.put(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_E,
                        InputEvent.CTRL_DOWN_MASK
                ),
                "exportarCSV"
        );

        mapaAcciones.put("exportarCSV", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                delegado.getBtnExportar().doClick();
            }
        });

        // CTRL + F: BUSCAR CONTACTO
        mapaTeclas.put(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_F,
                        InputEvent.CTRL_DOWN_MASK
                ),
                "buscarContacto"
        );

        mapaAcciones.put("buscarContacto", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                delegado.getTxtBuscarContactos()
                        .requestFocusInWindow();

                delegado.getTxtBuscarContactos().selectAll();
            }
        });

        // SUPR: ELIMINAR CONTACTO SELECCIONADO
        // Solo funciona cuando la tabla tiene el foco.
        InputMap mapaTabla = delegado.getTblContactos()
                .getInputMap(JComponent.WHEN_FOCUSED);

        mapaTabla.put(
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
                "eliminarContacto"
        );

        delegado.getTblContactos()
                .getActionMap()
                .put("eliminarContacto", new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        eliminarContacto();
                    }
                });
    }

    // CARGAR CONTACTOS DESDE CSV
    // CARGAR CONTACTOS CON BARRA DE PROGRESO
    private void cargarContactosRegistrados() {

        // Obtener la barra de progreso
        JProgressBar barra = delegado.getPrgCarga();

        // Configuración inicial
        barra.setMinimum(0);
        barra.setMaximum(100);
        barra.setValue(0);
        barra.setStringPainted(true);
        barra.setString("Cargando contactos...");
        barra.setIndeterminate(true);

        // Evitar operaciones mientras se cargan los datos
        delegado.getBtnAgregar().setEnabled(false);
        delegado.getBtnModificar().setEnabled(false);
        delegado.getBtnEliminar().setEnabled(false);
        delegado.getBtnExportar().setEnabled(false);

        // Crear tarea en segundo plano
        SwingWorker<List<persona>, Void> trabajador
                = new SwingWorker<List<persona>, Void>() {

            @Override
            protected List<persona> doInBackground()
                    throws Exception {

                // Leer los datos desde el DAO
                return dao.leerArchivo();
            }

            @Override
            protected void done() {

                barra.setIndeterminate(false);

                try {
                    // Obtener los contactos cargados
                    List<persona> datosCargados = get();

                    // Quitar selección anterior
                    delegado.getTblContactos().clearSelection();

                    // Actualizar lista de contactos
                    contactos = datosCargados;

                    // Limpiar la tabla
                    modeloTabla.setRowCount(0);

                    // Agregar contactos a la JTable
                    for (persona contacto : contactos) {

                        Object[] fila = {
                            contacto.getNombre(),
                            contacto.getTelefono(),
                            contacto.getEmail(),
                            contacto.getCategoria(),
                            contacto.isFavorito()
                        };

                        modeloTabla.addRow(fila);
                    }

                    // Actualizar estadísticas
                    actualizarEstadisticas();

                    // Carga completada
                    barra.setValue(100);
                    barra.setString("Carga completada: "
                            + contactos.size() + " contactos");

                } catch (InterruptedException ex) {

                    Thread.currentThread().interrupt();

                    barra.setValue(0);
                    barra.setString("Carga interrumpida");

                    JOptionPane.showMessageDialog(
                            delegado,
                            "La carga fue interrumpida."
                    );

                } catch (ExecutionException ex) {

                    barra.setValue(0);
                    barra.setString("Error de carga");

                    JOptionPane.showMessageDialog(
                            delegado,
                            "Error al cargar contactos: "
                            + ex.getCause().getMessage()
                    );

                } finally {

                    // Habilitar nuevamente los botones
                    delegado.getBtnAgregar().setEnabled(true);
                    delegado.getBtnModificar().setEnabled(true);
                    delegado.getBtnEliminar().setEnabled(true);
                    delegado.getBtnExportar().setEnabled(true);
                }
            }
        };

        // Iniciar la tarea
        trabajador.execute();
    }

    // LEER DATOS DE LA INTERFAZ
    private persona obtenerContactoFormulario() {

        String nombre = delegado.getTxtNombres()
                .getText().trim();

        String telefono = delegado.getTxtTelefono()
                .getText().trim();

        String email = delegado.getTxtEmail()
                .getText().trim();

        String categoria = String.valueOf(
                delegado.getCmbCategorias().getSelectedItem()
        );

        boolean favorito = delegado.getChbFavorito()
                .isSelected();

        if (nombre.isEmpty()
                || telefono.isEmpty()
                || email.isEmpty()) {

            JOptionPane.showMessageDialog(
                    delegado,
                    "Todos los campos deben estar llenos."
            );

            return null;
        }

        if (delegado.getCmbCategorias().getSelectedIndex() == 0
                || categoria.trim().isEmpty()) {

            JOptionPane.showMessageDialog(
                    delegado,
                    "Seleccione una categoría."
            );

            return null;
        }

        return new persona(
                nombre,
                telefono,
                email,
                categoria,
                favorito
        );
    }

    // MANEJO DE LOS BOTONES
    @Override
    public void actionPerformed(ActionEvent e) {

        if (dao == null) {
            JOptionPane.showMessageDialog(
                    delegado,
                    "El almacenamiento no está disponible."
            );
            return;
        }

        Object origen = e.getSource();

        if (origen == delegado.getBtnAgregar()) {
            agregarContacto();

        } else if (origen == delegado.getBtnModificar()) {
            modificarContacto();

        } else if (origen == delegado.getBtnEliminar()) {
            eliminarContacto();

        } else if (origen == delegado.getBtnExportar()) {
            exportarContactos();
        }
    }

    // AGREGAR CONTACTO
    private void agregarContacto() {

        persona contacto = obtenerContactoFormulario();

        if (contacto == null) {
            return;
        }

        try {
            dao.escribirArchivo(contacto);

            cargarContactosRegistrados();
            limpiarCampos();

            JOptionPane.showMessageDialog(
                    delegado,
                    "Contacto registrado correctamente."
            );

        } catch (IOException ex) {
            mostrarError(ex);
        }
    }

    // MODIFICAR CONTACTO
    private void modificarContacto() {

        int fila = obtenerIndiceSeleccionado();

        if (fila == -1) {
            JOptionPane.showMessageDialog(
                    delegado,
                    "Seleccione un contacto para modificar."
            );
            return;
        }

        persona contacto = obtenerContactoFormulario();

        if (contacto == null) {
            return;
        }

        List<persona> actualizados
                = new ArrayList<>(contactos);

        actualizados.set(fila, contacto);

        try {
            dao.actualizarContactos(actualizados);

            cargarContactosRegistrados();
            limpiarCampos();

            JOptionPane.showMessageDialog(
                    delegado,
                    "Contacto modificado correctamente."
            );

        } catch (IOException ex) {
            mostrarError(ex);
        }
    }

    // ELIMINAR CONTACTO
    private void eliminarContacto() {

        int fila = obtenerIndiceSeleccionado();

        if (fila == -1) {
            JOptionPane.showMessageDialog(
                    delegado,
                    "Seleccione un contacto para eliminar."
            );
            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(
                delegado,
                "¿Desea eliminar este contacto?",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        List<persona> actualizados
                = new ArrayList<>(contactos);

        actualizados.remove(fila);

        try {
            dao.actualizarContactos(actualizados);

            cargarContactosRegistrados();
            limpiarCampos();

            JOptionPane.showMessageDialog(
                    delegado,
                    "Contacto eliminado correctamente."
            );

        } catch (IOException ex) {
            mostrarError(ex);
        }
    }

    // EXPORTAR CONTACTOS A CSV
    private void exportarContactos() {

        // Crear ventana para seleccionar archivo
        JFileChooser selector = new JFileChooser();

        selector.setDialogTitle("Exportar contactos");

        // Filtrar archivos CSV
        selector.setFileFilter(
                new FileNameExtensionFilter("Archivo CSV", "csv")
        );

        // Nombre predeterminado
        selector.setSelectedFile(new File("contactos.csv"));

        // Mostrar ventana para guardar archivo
        int resultado = selector.showSaveDialog(delegado);

        if (resultado == JFileChooser.APPROVE_OPTION) {

            File destino = selector.getSelectedFile();

            // Agregar extensión CSV si no existe
            if (!destino.getName().toLowerCase().endsWith(".csv")) {
                destino = new File(destino.getAbsolutePath() + ".csv");
            }

            try {
                // Evitar sobrescribir el archivo original
                File original = new File(
                        System.getProperty("user.home"),
                        "gestionContactos/datosContactos.csv"
                );

                if (destino.getCanonicalFile()
                        .equals(original.getCanonicalFile())) {

                    JOptionPane.showMessageDialog(
                            delegado,
                            "No puede exportar sobre el archivo original."
                    );
                    return;
                }

                // Confirmar si el archivo de destino ya existe
                if (destino.exists()) {

                    int confirmar = JOptionPane.showConfirmDialog(
                            delegado,
                            "El archivo ya existe. ¿Desea reemplazarlo?",
                            "Confirmar exportación",
                            JOptionPane.YES_NO_OPTION
                    );

                    if (confirmar != JOptionPane.YES_OPTION) {
                        return;
                    }
                }

                // Llamar al método del DAO
                dao.exportarCSV(destino);

                JOptionPane.showMessageDialog(
                        delegado,
                        "Contactos exportados correctamente."
                );

            } catch (IOException ex) {
                mostrarError(ex);
            }
        }
    }

    // OBTENER FILA SELECCIONADA
    private int obtenerIndiceSeleccionado() {

        int filaVista = delegado.getTblContactos()
                .getSelectedRow();

        if (filaVista == -1) {
            return -1;
        }

        // Convertir índice visual a índice del modelo
        return delegado.getTblContactos()
                .convertRowIndexToModel(filaVista);
    }

    // SELECCIONAR CONTACTO EN JTABLE
    @Override
    public void valueChanged(ListSelectionEvent e) {

        if (e.getValueIsAdjusting()) {
            return;
        }

        int indice = obtenerIndiceSeleccionado();

        if (indice >= 0 && indice < contactos.size()) {

            persona contacto = contactos.get(indice);

            delegado.getTxtNombres()
                    .setText(contacto.getNombre());

            delegado.getTxtTelefono()
                    .setText(contacto.getTelefono());

            delegado.getTxtEmail()
                    .setText(contacto.getEmail());

            delegado.getCmbCategorias()
                    .setSelectedItem(contacto.getCategoria());

            delegado.getChbFavorito()
                    .setSelected(contacto.isFavorito());
        }
    }

    // LIMPIAR CAMPOS
    private void limpiarCampos() {

        delegado.getTblContactos().clearSelection();

        delegado.getTxtNombres().setText("");
        delegado.getTxtTelefono().setText("");
        delegado.getTxtEmail().setText("");

        delegado.getCmbCategorias().setSelectedIndex(0);
        delegado.getChbFavorito().setSelected(false);
    }

    // CONFIGURAR BÚSQUEDA
    private void configurarBusqueda() {

        delegado.getTxtBuscarContactos()
                .getDocument()
                .addDocumentListener(new DocumentListener() {

                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        filtrarContactos();
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        filtrarContactos();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        filtrarContactos();
                    }
                });
    }

    // FILTRAR CONTACTOS POR NOMBRE
    private void filtrarContactos() {

        String texto = delegado.getTxtBuscarContactos()
                .getText().trim();

        if (texto.isEmpty()) {
            ordenador.setRowFilter(null);
        } else {
            ordenador.setRowFilter(
                    javax.swing.RowFilter.regexFilter(
                            "(?i)" + Pattern.quote(texto),
                            0
                    )
            );
        }
    }

    // ACTUALIZAR ESTADÍSTICAS
    private void actualizarEstadisticas() {

        int familia = 0;
        int amigos = 0;
        int trabajo = 0;
        int favoritos = 0;

        for (persona contacto : contactos) {

            switch (contacto.getCategoria()) {

                case "Familia":
                    familia++;
                    break;

                case "Amigos":
                    amigos++;
                    break;

                case "Trabajo":
                    trabajo++;
                    break;
            }

            if (contacto.isFavorito()) {
                favoritos++;
            }
        }

        delegado.getLblTotalNumero()
                .setText(String.valueOf(contactos.size()));

        delegado.getLblFamiliaNumero()
                .setText(String.valueOf(familia));

        delegado.getLblAmigosNumero()
                .setText(String.valueOf(amigos));

        delegado.getLblTrabajoNumero()
                .setText(String.valueOf(trabajo));

        delegado.getLblFavoritosNumero()
                .setText(String.valueOf(favoritos));
    }

    // MOSTRAR ERRORES DEL DAO
    private void mostrarError(IOException ex) {

        JOptionPane.showMessageDialog(
                delegado,
                "Error de almacenamiento: "
                + ex.getMessage()
        );
    }
}
