package org.kilocraft.essentials;

import java.io.IOException;
import java.util.Properties;

public class Mod {
    public static Properties properties = new Properties();
    public static Properties messages = new Properties();

    public Mod() {
        try {
            //properties.load(Mod.class.getResource("Mod.properties").openStream());
            messages.load(Mod.class.getClassLoader().getResourceAsStream("Messages.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
