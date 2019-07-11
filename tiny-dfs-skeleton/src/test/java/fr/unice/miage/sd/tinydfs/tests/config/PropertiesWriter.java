package fr.unice.miage.sd.tinydfs.tests.config;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

public class PropertiesWriter {

    // Usage: java fr.unice.miage.sd.tinydfs.tests.config.PropertiesWriter storage_service_name registry_host
    public static void main(String[] args) {
        String serviceName = args[0];
        String registryHost = args[1];
        Properties prop = new Properties();
        OutputStream output = null;
        try {
            output = new FileOutputStream(PropertiesWriter.class.getResource(
                    Constants.PROPERTIES_FILE_PATH).getFile());
            prop.setProperty(Constants.SERVICE_NAME_PROPERTY_KEY, serviceName);
            prop.setProperty(Constants.REGISTRY_HOST_PROPERTY_KEY, registryHost);
            prop.store(output, null);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("[PropertiesWriter] Properties written.");
            }

        }

    }

}
