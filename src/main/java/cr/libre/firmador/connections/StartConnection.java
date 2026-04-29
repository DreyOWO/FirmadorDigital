package cr.libre.firmador.connections;

import cr.libre.firmador.gui.GUIInterface;
import java.util.ArrayList;
import java.util.List;

public class StartConnection {

    //private GUIInterface gui; //FIXME unused?
    //private static final String FILE_NAME = "servicesUrls.xml"; // FIXME unused?

    public StartConnection(GUIInterface gui) {
        //this.gui = gui;
    }
    public List<Connection> startConnection(){

        List<Connection> connections = new ArrayList<>();
        Connection firmadorRemoto = new Connection("Firmador Remoto","Firmador Remoto", 3516, null);
        Connection fva_speaker = new Connection("Gaudi","Gaudi", 0, null);
        connections.add(firmadorRemoto);
        connections.add(fva_speaker);
        return connections;
    }
}
