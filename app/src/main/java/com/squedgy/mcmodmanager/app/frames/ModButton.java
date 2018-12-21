package com.squedgy.mcmodmanager.app.frames;

import javax.swing.*;
import com.squedgy.mcmodmanager.api.abstractions.*;

public class ModButton extends JButton{

    public ModVersion mod;

    public ModButton(ModVersion mod){
        super(mod.getModName());
        this.mod = mod;
    }

}
