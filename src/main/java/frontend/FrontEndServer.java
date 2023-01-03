package frontend;

import javax.xml.ws.Endpoint;

public class FrontEndServer {

    private static String[] cities = {"MTL", "TOR", "VAN"};

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Information is missing");
            System.exit(1);
        }

        String currentIP = args[0];
        String sequencerAddress = args[1];
        int sequencerPort = Integer.parseInt(args[2]);

        for (String city : cities) {
            FrontEndImpl frontEnd = new FrontEndImpl();
            frontEnd.setSequencerAddress(sequencerAddress);
            frontEnd.setSequencerPort(sequencerPort);
            frontEnd.setCity(city);

            Endpoint endpoint = Endpoint.publish(String.format("http://%s:9001/frontend-%s", currentIP, city), frontEnd);
            System.out.println("Frontend ready and waiting..." + endpoint.isPublished());
        }
    }
}
