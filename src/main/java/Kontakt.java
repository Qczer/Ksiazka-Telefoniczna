import java.util.*;

public class Kontakt {
    Map<String, Object> content;

    Kontakt(String name, String locality, ArrayList<String> numbers) {
        this.content = Map.of("nazwa", name, "miejscowosc", locality, "numery", numbers);
    }
}