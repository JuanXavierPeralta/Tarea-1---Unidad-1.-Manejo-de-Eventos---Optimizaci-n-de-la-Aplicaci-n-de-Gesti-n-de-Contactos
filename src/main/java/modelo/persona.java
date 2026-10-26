package modelo;

//Definición de la clase pública "persona"
public class persona {

    // Declaración de variables privadas de la clase "persona"
    private String nombre, telefono, email, categoria;
    private boolean favorito;

    // Constructor público de la clase "persona"
    public persona() {
        super(); // Llama al constructor de la clase base (Object)
        //Inicializa las variables 
        this.nombre = "";
        this.telefono = "";
        this.email = "";
        this.categoria = "";
        this.favorito = false;
    }

    // Constructor público de la clase "persona" que inicializa todos los campos 
    public persona(String nombre, String telefono, String email, String categoria, boolean favorito) {
        super();// Llama al constructor de la clase base (Object)
        //Inicializa las variables con los valores enviado por los argumentos
        this.nombre = nombre;
        this.telefono = telefono;
        this.email = email;
        this.categoria = categoria;
        this.favorito = favorito;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public boolean isFavorito() {
        return favorito;
    }

    public void setFavorito(boolean favorito) {
        this.favorito = favorito;
    }

    // Método para proveer un formato para almacenar en un archivo
    public String datosContacto() {
        // Estructurar el siguiente formato: nombre;telefono;email;categoria;favorito
        // Por ejemplo: Daniela Poma;097145478;dpoma2024@gmail.com;amigo;true
        String contacto = String.format("%s;%s;%s;%s;%s", nombre, telefono, email, categoria, favorito); // Crea una cadena formateada con los valores de las variables
        return contacto; // Retorna la cadena formateada
    }
}
