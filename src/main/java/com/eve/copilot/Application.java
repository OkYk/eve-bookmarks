package com.eve.copilot;

import com.google.gson.Gson;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
                }

            }
            Thread.sleep(1000);
        }
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
