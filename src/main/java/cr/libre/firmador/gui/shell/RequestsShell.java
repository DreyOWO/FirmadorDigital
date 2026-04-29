package cr.libre.firmador.gui.shell;

import cr.libre.firmador.Settings;

import java.util.List;

public class RequestsShell {

    public static class ListSignRemoteCommand {
        List<SignRemoteCommand> commands;
        String serialnumber;
        private String fileOutput;
        public ListSignRemoteCommand(){}
        public ListSignRemoteCommand(List<SignRemoteCommand> commands, String serialnumber, String fileOutput) {
            this.fileOutput = fileOutput;
            this.serialnumber = serialnumber;
            this.commands = commands;
        }
        public List<SignRemoteCommand> getCommands() {
            return commands;
        }
        public String getSerialnumber() {
            return serialnumber;
        }
        public String getFileOutput() {
            return fileOutput;
        }
    }

    public static class SignRemoteCommand {
        private String base64Document;
        private String documentName;
        private Settings settings;
        private String externalId;
        public SignRemoteCommand(){}

        public SignRemoteCommand(String base64Document, String documentName, String serialnumber, Settings settings, String externalId) {
            this.base64Document = base64Document;
            this.documentName = documentName;
            this.settings = settings;
            this.externalId = externalId;
        }

        public String getBase64Document() {
            return base64Document;
        }
        public void setBase64Document(String base64Document) {
            this.base64Document = base64Document;
        }
        public String getDocumentName() {
            return documentName;
        }
        public void setDocumentName(String documentName) {
            this.documentName = documentName;
        }
        public Settings getSettings() {
            return settings;
        }
        public void setSettings(Settings settings) {
            this.settings = settings;
        }
        public String getExternalId() {
            return externalId;
        }
    }

    public static class ListSignCommand {
        List<SignCommand> commands;
        String fileOutput;

        public ListSignCommand(){}
        public ListSignCommand(List<SignCommand> commands, String fileOutput, String filePath) {
            this.fileOutput = fileOutput;
            this.commands = commands;
        }
        public List<SignCommand> getCommands() {
            return commands;
        }
        public String getFileOutput() {
            return fileOutput;
        }
    }

    public static class SignCommand {
        public String externalId;
        public Settings settings;
        public String filePath;
        public SignCommand(){}

        public SignCommand(String externalId, String filePath, String pin, Settings settings, String fileOutput) {
            this.externalId = externalId;
            this.settings = settings;
        }
        public Settings getSettings() {
            return settings;
        }
        public void setSettings(Settings settings) {
            this.settings = settings;
        }
        public String getExternalId() {
            return externalId;
        }
    }

    public static class ListValidateCommand {
        List<ValidateCommand> commands;
        public String fileOutput;
        public ListValidateCommand(){}
        public ListValidateCommand(List<ValidateCommand> commands, String fileOutput, String filePath) {
            this.fileOutput = fileOutput;
            this.commands = commands;
        }
        public List<ValidateCommand> getCommands() {
            return commands;
        }
        public String getFileOutput() {
            return fileOutput;
        }
    }

    public static class ValidateCommand {
        String externalId;
        String filePath;

        public ValidateCommand(){}

        public ValidateCommand(String documentPath, String externalId) {
            this.externalId = externalId;
            this.filePath = documentPath;
        }
        public String getFilePath() {
            return filePath;
        }
        public String getExternalId() {
            return externalId;
        }
    }

    public static class PreviewCommand {
        String filePath;
        String fileOutput;
        String base64Document;

        public PreviewCommand(){}

        public PreviewCommand(String filePath, String fileOutput) {
            this.filePath = filePath;
            this.fileOutput = fileOutput;
        }
        public String getFilePath() {
            return filePath;
        }
        public String getFileOutput() {
            return fileOutput;
        }
        public String getBase64Document() {
            return base64Document;
        }
    }
}


