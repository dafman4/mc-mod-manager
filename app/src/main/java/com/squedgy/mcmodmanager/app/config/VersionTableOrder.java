package com.squedgy.mcmodmanager.app.config;

import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import javafx.scene.control.TableColumn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class VersionTableOrder {

    private static final String TABLE_CONFIG = Config.CONFIG_DIRECTORY + "mod-table.json";
    private static final Map<String,Integer> order = readProps();

    protected VersionTableOrder() throws Exception { }

    private static Map<String,Integer> readProps() {
        try {
            return Config.readProps(TABLE_CONFIG)
                    .entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> Integer.valueOf(e.getValue())));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static int compareColumns(String a, String b){
        Integer one = order.get(a),
            two = order.get(b);

        if(one == null && two == null) return 0;
        else if (one == null) return -1;
        else if (two == null) return 1;

        return one.compareTo(two);
    }

    public static void writeColumnOrder(List<TableColumn<ModVersion, ?>> order){
        Map<String,String> props = new HashMap<>();
        for(int i = 0; i < order.size(); i++) props.put(order.get(i).getText(), String.valueOf(i));
        Config.writeProps(TABLE_CONFIG, props);
    }
}
