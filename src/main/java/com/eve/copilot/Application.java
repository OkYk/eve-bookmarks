package com.eve.copilot;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.IOException;

public class Application {

    private static String bookmarkFromScanLine(String scanLine){
        if (!scanLine.matches("[A-Z]{3}-[0-9]{3}\t[A-Za-z ]+\t[A-Za-z ]+\t[A-Za-z -0-9]+\t100.0%\t[0-9\\.,]+ [A-Za-z]+$")) {
            return null;
        }

        String[] data_parts = scanLine.split("\t");
        char type = data_parts[2].charAt(0);
        if (type=='W'){
            return data_parts[0].substring(0, 3) + "-C?-K162";
        } else {
            return data_parts[0].substring(0, 3) + "-" + data_parts[2].charAt(0) + "-" + data_parts[3];
        }
    }

    public static void main(String args[]) throws InterruptedException {
        String data = "";
        while (true) {
            String clipboardContents = getClipboardContents();
            if (!data.equalsIgnoreCase(clipboardContents)) {
                data = clipboardContents;

                if (!data.contains("\n")) {
                    final String bookmark = bookmarkFromScanLine(data);
                    if (bookmark!=null){
                        StringSelection selection = new StringSelection(bookmark);
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                        System.out.println("set: "+bookmark);
                    }
                }

            }
            Thread.sleep(1000);
        }
    }

    public static String getClipboardContents() {
        String result = "";
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        Transferable contents = clipboard.getContents(null);
        boolean hasTransferableText =
                (contents != null)
                        && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
        if (hasTransferableText) {
            try {
                result = (String) contents.getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException ex) {
                System.out.println(ex);
                ex.printStackTrace();
            } catch (IOException ex) {
                System.out.println(ex);
                ex.printStackTrace();
            }
        }
        return result;
    }
}
