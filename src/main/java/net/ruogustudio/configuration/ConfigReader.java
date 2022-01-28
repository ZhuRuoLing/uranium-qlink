package net.ruogustudio.configuration;

import com.google.gson.Gson;
import net.ruogustudio.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class ConfigReader {
    private static final Logger logger = LoggerFactory.getLogger("ConfigReader");

    @Nullable
    public static Configuration read(){
        logger.info("Reading Config file.");
        char[] buf = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new Util().getWorkingDir() + File.separator + "config.json"));
            Gson json = new Gson();
            Configuration config = json.fromJson(reader,Configuration.class);
            if (!(config == null)){
                return config;
            }
            else {
                return null;
            }
        } catch (Exception e) {
            logger.error("Exception:" + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
