package com.squedgy.mcmodmanager.app.frames;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame {

    private static final String DEFAULT_TITLE = "Mc Mod Manager";

    public MainWindow(String title, GraphicsConfiguration gc) {
        super(title, gc);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    }

    public MainWindow(String title){
        this(title, null);
    }

    public MainWindow(GraphicsConfiguration gc){
        this(DEFAULT_TITLE, gc);
    }
    public MainWindow(){
        this(DEFAULT_TITLE);
    }

    @Override
    protected void frameInit() {
        super.frameInit();
    }
}
