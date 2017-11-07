package com.eve.copilot;

import com.eve.copilot.dto.LootEntry;
import com.eve.copilot.dto.Signature;
import com.eve.copilot.dto.Tradehub;
import com.google.gson.Gson;
import fr.guiguilechat.eveonline.database.EveDatabase;
import fr.guiguilechat.eveonline.database.esi.ESIMarket;
import fr.guiguilechat.eveonline.database.yaml.MetaInf;
import fr.guiguilechat.eveonline.database.yaml.YamlDatabase;

import java.awt.datatransfer.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class BookmarkManager {

    private final Map<String, String> sigDB = loadSigs();
    private final Set<String> hubDB = loadHubs();
    private final EveDatabase evedb = new YamlDatabase();
    private final ESIMarket esimarket = evedb.ESIRegion("TheForge");
    private final Pattern SCAN_RESULT_PATTERN = Pattern.compile("[A-Z]{3}-[0-9]{3}\t[A-Za-z ]+\t[A-Za-z ]+\t[A-Za-z -0-9]+\t100.0%\t[0-9\\.,]+ [A-Za-z]+$");
    private final Pattern SIG_PATTERN = Pattern.compile("[A-Z]{3}-C\\?-[A-Za-z][0-9]+");
    private boolean probeResultEnabled = true;
    private boolean sigEnabled = true;
    private boolean hubDistanceEnabled = true;
    private boolean enabled = true;
    private static final DecimalFormat priceFormatter = new DecimalFormat("#0.0");

    /**
     *
     * @param value
     * @return
     */
    private static String formatPrice(double value) {
        if (value >= 1e12) {
            return priceFormatter.format(value / 1e12) + "T";
        } else if (value >= 1e9) {
            return priceFormatter.format(value / 1e9) + "G";
        } else if (value >= 1e6) {
            return priceFormatter.format(value / 1e6) + "M";
        } else if (value >= 1e3) {
            return priceFormatter.format(value / 1e3) + "k";
        } else {
            return priceFormatter.format(value);
        }
    }


    private Map<String, String> loadSigs() {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream("wh_sig.json");

        Gson g = new Gson();
        Signature[] sigDb = g.fromJson(new BufferedReader(new InputStreamReader(is)), Signature[].class);
        System.out.println("Signatures loaded: " + sigDb.length);
        return Arrays.stream(sigDb).collect(Collectors.toMap(o -> o.sig, o -> o.val));
    }

    private Set<String> loadHubs() {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream("trade_hubs.json");

        Gson g = new Gson();
        Tradehub[] hubDb = g.fromJson(new BufferedReader(new InputStreamReader(is)), Tradehub[].class);
        System.out.println("Hubs loaded: " + hubDb.length);
        return Arrays.stream(hubDb).map(Tradehub::getHubname).collect(Collectors.toSet());
    }

    private String bookmarkFromScanLine(String scanLine) {
        if (!probeResultEnabled || !SCAN_RESULT_PATTERN.matcher(scanLine).matches()) {
            return null;
        }

        String[] data_parts = scanLine.split("\t");
        char type = data_parts[2].charAt(0);
        if (type == 'W') {
            return data_parts[0].substring(0, 3) + "-C?-K162";
        } else {
            return data_parts[0].substring(0, 3) + "-" + data_parts[2].charAt(0) + "-" + data_parts[3];
        }
    }

    private String expandSig(String scanLine, Map<String, String> sigDB) {
        if (!sigEnabled || !SIG_PATTERN.matcher(scanLine).matches()) {
            return null;
        }

        final String[] data_parts = scanLine.split("-");
        if (sigDB.containsKey(data_parts[2])) {
            final String val = sigDB.get(data_parts[2]);
            return data_parts[0] + "-" + val + "-" + data_parts[2];
        }

        return null;
    }

    boolean bmClipboard(Transferable trans, Clipboard c, UIController ui) {
        if (enabled && trans.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                String clipText = (String) trans
                        .getTransferData(DataFlavor.stringFlavor);
                if (!clipText.contains("\n")) {
                    final String bookmark = bookmarkFromScanLine(clipText);
                    if (bookmark != null) {
                        StringSelection selection = new StringSelection(bookmark);
                        c.setContents(selection, selection);
                        System.out.println("set: " + bookmark);
                        return true;
                    }
                    final String sigExpansion = expandSig(clipText, sigDB);
                    if (sigExpansion != null) {
                        StringSelection selection = new StringSelection(sigExpansion);
                        c.setContents(selection, selection);
                        System.out.println("set: " + sigExpansion);
                        return true;
                    }
                } else if (clipText.contains("Jita")) {
                    final String hubExpansion = expandHub(clipText, hubDB);
                    if (hubExpansion != null && !hubExpansion.isEmpty()) {
                        StringSelection selection = new StringSelection(hubExpansion);
                        c.setContents(selection, selection);
                        System.out.println("set: " + hubExpansion);
                        return true;
                    }
                }

                if (clipText.matches("([0-9]+ [^\n]+\n)*([0-9]+ [^\n]+)")) {
                    final double sum = Arrays.stream(clipText.split("\r?\n")).map((String line) -> {
                        final String[] split = line.split(" ", 2);
                        if (split.length != 2) {
                            return LootEntry.BLANK;
                        }
                        return new LootEntry(Integer.parseInt(split[0]), split[1]);
                    }).mapToDouble(entry -> {
                        if (!LootEntry.BLANK.equals(entry)) {
                            final MetaInf metaInf = evedb.getMetaInfs().get(entry.getName());
                            if (metaInf != null && entry.getQuantity() != 0) {
                                return esimarket.getBO(metaInf.id, entry.getQuantity());
                            }
                        }
                        return 0D;
                    }).sum();
                    ui.message("Price: "+formatPrice(sum));
                }
            } catch (UnsupportedFlavorException | IOException e) {
                e.printStackTrace();
            }

        }
        return false;
    }

    private String expandHub(String data, Set<String> hubDB) {
        if (!hubDistanceEnabled || !data.contains("Coordinate\t")) {
            return null;
        }

        final Map<String, Integer> distance = new LinkedHashMap<>();
        final String[] data_parts = data.split("\n");
        for (String part : data_parts) {
            for (String hub : hubDB) {
                if (part.toLowerCase().contains(hub)) {
                    final Matcher hubMatcher = Pattern.compile("Coordinate\t([0-9]+)").matcher(part);
                    if (hubMatcher.find()) {
                        distance.put(hub, Integer.valueOf(hubMatcher.group(1)));
                    }
                }
            }
        }
        return distance.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getValue))
                .map(o -> o.getKey() + ": " + o.getValue() + "\n")
                .collect(Collectors.joining());
    }

    void setProbeResultEnabled(boolean probeResultEnabled) {
        this.probeResultEnabled = probeResultEnabled;
    }

    void setSigEnabled(boolean sigEnabled) {
        this.sigEnabled = sigEnabled;
    }

    void setHubDistanceEnabled(boolean hubDistanceEnabled) {
        this.hubDistanceEnabled = hubDistanceEnabled;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
