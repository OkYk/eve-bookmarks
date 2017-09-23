package com.eve.copilot;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;

public class Application extends Thread implements ClipboardOwner {

    private static final Clipboard SYS_CLIP = Toolkit.getDefaultToolkit().getSystemClipboard();
    private static final BookmarkManager BM_MANAGER = new BookmarkManager();

    @Override
    public void run() {
        new UIController(BM_MANAGER);
        Transferable trans = SYS_CLIP.getContents(this);
        TakeOwnership(trans);
    }

    @Override
    public void lostOwnership(Clipboard c, Transferable t) {
        try {
            sleep(250);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Transferable contents = SYS_CLIP.getContents(this);
        try {
            if (BM_MANAGER.bmClipboard(contents, c)) {
                contents = SYS_CLIP.getContents(this);
            }
            TakeOwnership(contents);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void TakeOwnership(Transferable t) {
        SYS_CLIP.setContents(t, this);
    }

    public static void main(String args[]) throws InterruptedException {
        new Application().start();

        while (true) {
            Thread.sleep(60000L);
        }
    }
}
