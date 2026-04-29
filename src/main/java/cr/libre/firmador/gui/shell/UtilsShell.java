package cr.libre.firmador.gui.shell;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class UtilsShell {
    public static RequestsShell.ListValidateCommand parseValidate(String jsonPath) {
        try {
            String json = new String(Files.readAllBytes(Paths.get(jsonPath)));

            ObjectMapper mapper = new ObjectMapper();
            RequestsShell.ListValidateCommand validateCommand = mapper.readValue(json, RequestsShell.ListValidateCommand.class);

            if (validateCommand == null) {
                System.err.println("ERROR: JSON inválido o incompleto");
                return null;
            }

            return validateCommand;
        } catch (IOException e) {
            System.err.println("Error leyendo archivo JSON: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Error parseando JSON de settings: " + e.getMessage());
            e.printStackTrace(); // Para ver más detalles del error
            return null;
        }
    }

    public static RequestsShell.ListSignRemoteCommand parseSignRemoteCommand(String jsonPath) {
        try {
            String json = new String(Files.readAllBytes(Paths.get(jsonPath)));

            ObjectMapper mapper = new ObjectMapper();
            RequestsShell.ListSignRemoteCommand signRemoteCommand = mapper.readValue(json, RequestsShell.ListSignRemoteCommand.class);

            if (signRemoteCommand == null) {
                System.err.println("ERROR: JSON inválido o incompleto");
                return null;
            }
            return signRemoteCommand;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static RequestsShell.ListSignCommand parseSingCommand(String jsonPath) {
        try {
            // Leer el archivo JSON
            String json = new String(Files.readAllBytes(Paths.get(jsonPath)));

            ObjectMapper mapper = new ObjectMapper();
            RequestsShell.ListSignCommand signCommand = mapper.readValue(json, RequestsShell.ListSignCommand.class);

            if (signCommand == null) {
                System.err.println("ERROR: JSON inválido o incompleto");
                return null;
            }

            return signCommand;
        } catch (IOException e) {
            System.err.println("Error leyendo archivo JSON: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Error parseando JSON de settings: " + e.getMessage());
            e.printStackTrace(); // Para ver más detalles del error
            return null;
        }
    }

    public static RequestsShell.PreviewCommand parsePreviewCommand(String jsonPath) {
        try {
            // Leer el archivo JSON
            String json = new String(Files.readAllBytes(Paths.get(jsonPath)));

            ObjectMapper mapper = new ObjectMapper();
            RequestsShell.PreviewCommand previewCommand = mapper.readValue(json, RequestsShell.PreviewCommand.class);

            if (previewCommand == null) {
                System.err.println("ERROR: JSON inválido o incompleto");
                return null;
            }

            return previewCommand;
        } catch (IOException e) {
            System.err.println("Error leyendo archivo JSON: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Error parseando JSON de settings: " + e.getMessage());
            e.printStackTrace(); // Para ver más detalles del error
            return null;
        }
    }
}
