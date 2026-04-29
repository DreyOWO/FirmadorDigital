package cr.libre.firmador.remote;

import java.util.List;

public class RemoteMultipleSignatureValueDTO {
    public List<RemoteSignatureValueDTO> signatures;

    public RemoteMultipleSignatureValueDTO() {}

    public List<RemoteSignatureValueDTO> getSignatures() {
        return signatures;
    }

    public void setSignatures(List<RemoteSignatureValueDTO> signatures) {
        this.signatures = signatures;
    }
}
