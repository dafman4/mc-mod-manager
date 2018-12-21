package com.squedgy.mcmodmanager.app.frames;

import javax.swing.*;
import java.awt.*;

public class ModList extends JPanel {



    public ModList(LayoutManager layout, boolean isDoubleBuffered) {
        super(layout, isDoubleBuffered);
    }

    public ModList(LayoutManager layout) {
        super(layout);
    }

    public ModList(boolean isDoubleBuffered) {
        super(isDoubleBuffered);
    }

    public ModList() {
        super();
    }

    @Override
    public void update(Graphics g) {
        super.update(g);


    }
}
