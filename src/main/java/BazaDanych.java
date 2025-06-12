import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;

import java.util.ArrayList;
import java.util.Map;

public class BazaDanych {
    File file;

    BazaDanych(String path) {
        this.file = new File(path);
    }

    public void write(ArrayList<Map<String, Object>> data) {
        ObjectMapper objectMapper = new ObjectMapper();

        // Write the data to the file
        try {
            objectMapper.writeValue(this.file, data);
        } catch (Exception error) {
            System.out.println("[ERROR] Nie można zapisać danych do pliku.");
        }
    }

    public ArrayList<Map<String, Object>> read() {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // Read the data from the file
            ArrayList<Map<String, Object>> data = objectMapper.readValue(this.file, new TypeReference<ArrayList<Map<String, Object>>>() {});

            return data;
        } catch (Exception error) {
            return new ArrayList<>();
        }
    }
}
