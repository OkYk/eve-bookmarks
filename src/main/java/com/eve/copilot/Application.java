package com.eve.copilot;

import com.google.gson.Gson;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Application {

    private static String bookmarkFromScanLine(String scanLine) {
        if (!scanLine.matches("[A-Z]{3}-[0-9]{3}\t[A-Za-z ]+\t[A-Za-z ]+\t[A-Za-z -0-9]+\t100.0%\t[0-9\\.,]+ [A-Za-z]+$")) {
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

    private static Map<String, String> loadSigs() {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream("wh_sig.json");

        Gson g = new Gson();
        Signature[] sigDb = g.fromJson(new BufferedReader(new InputStreamReader(is)), Signature[].class);
        System.out.println("Signatures loaded: " + sigDb.length);
        return Arrays.stream(sigDb).collect(Collectors.toMap(o -> o.sig, o -> o.val));
    }

    private static Set<String> loadHubs() {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream("trade_hubs.json");

        Gson g = new Gson();
        Tradehub[] hubDb = g.fromJson(new BufferedReader(new InputStreamReader(is)), Tradehub[].class);
        System.out.println("Hubs loaded: " + hubDb.length);
        return Arrays.stream(hubDb).map(Tradehub::getHubname).collect(Collectors.toSet());
    }

    private static String expandSig(String scanLine, Map<String, String> sigDB) {
        if (!scanLine.matches("[A-Z]{3}-C\\?-[A-Za-z][0-9]+")) {
            return null;
        }

        final String[] data_parts = scanLine.split("-");
        if (sigDB.containsKey(data_parts[2])) {
            final String val = sigDB.get(data_parts[2]);
            return data_parts[0] + "-" + val + "-" + data_parts[2];
        }

        return null;
    }

    public static void main(String args[]) throws InterruptedException {
        String data = "";
        Map<String, String> sigDB = loadSigs();
        Set<String> hubDB = loadHubs();
        while (true) {
            String clipboardContents = getClipboardContents();
            if (!data.equalsIgnoreCase(clipboardContents)) {
                data = clipboardContents;

                if (!data.contains("\n")) {
                    final String bookmark = bookmarkFromScanLine(data);
                    if (bookmark != null) {
                        StringSelection selection = new StringSelection(bookmark);
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                        System.out.println("set: " + bookmark);
                    }
                    final String sigExpansion = expandSig(data, sigDB);
                    if (sigExpansion != null) {
                        StringSelection selection = new StringSelection(sigExpansion);
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                        System.out.println("set: " + sigExpansion);
                    }
                } else if (data.contains("Jita")) {
                    final String hubExpansion = expandHub(data, hubDB);
                    StringSelection selection = new StringSelection(hubExpansion);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                    System.out.println("set: " + hubExpansion);
                }

            }
            Thread.sleep(1000);
        }
    }

    private static String expandHub(String data, Set<String> hubDB) {
        if (!data.contains("Coordinate")) {
            return null;
        }

        final Map<String, Integer> distance = new LinkedHashMap<>();
        final String[] data_parts = data.split("\n");
        for (String part : data_parts) {
            for (String hub : hubDB) {
                if (part.toLowerCase().contains(hub)){
                    final Matcher hubMatcher = Pattern.compile("Coordinate\t([0-9]+)").matcher(part);
                    if (hubMatcher.find()){
                        distance.put(hub, Integer.valueOf(hubMatcher.group(1)));
                    }
                }
            }
        }
        return distance.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getValue))
                .map(o -> o.getKey()+": "+o.getValue()+"\n")
                .collect(Collectors.joining());
    }

    private static String getClipboardContents() {
        String result = "";
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        Transferable contents = clipboard.getContents(null);
        boolean hasTransferableText =
                (contents != null)
                        && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
        if (hasTransferableText) {
            try {
                result = (String) contents.getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException | IOException ex) {
                System.out.println(ex);
                ex.printStackTrace();
            }
        }
        return result;
    }
}
