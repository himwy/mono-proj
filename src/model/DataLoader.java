package model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DataLoader {

    public static List<Slot> loadSlots(String path) throws IOException {
        List<Slot> slots = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split(",");
                if (parts.length < 5) {
                    throw new IllegalArgumentException(
                        "Malformed slot data on line " + lineNo + ": " + line);
                }
                try {
                    int number = Integer.parseInt(parts[0].trim());
                    String name = parts[1].trim();
                    int price = Integer.parseInt(parts[2].trim());
                    boolean land = Boolean.parseBoolean(parts[3].trim());
                    int side = Integer.parseInt(parts[4].trim());
                    slots.add(new Slot(number, name, price, land, side));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                        "Bad number on line " + lineNo + ": " + line, e);
                }
            }
        }
        slots.sort((a, b) -> Integer.compare(a.getNumber(), b.getNumber()));
        return slots;
    }
}
