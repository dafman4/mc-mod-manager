package com.squedgy.mcmodmanager.app.threads;

import com.squedgy.mcmodmanager.api.ModChecker;
import com.squedgy.mcmodmanager.api.abstractions.CurseForgeResponse;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class ModCheckingThread extends Thread {

    private final List<ModVersion> IDS;
    private final String mc;
    private final List<ModVersion> updateables = new LinkedList<>();
    private final Callback<List<ModVersion>,?> callback;

    public ModCheckingThread(List<ModVersion> modIds, String mcVersion, Callback<List<ModVersion>, ?> callback){
        IDS = new ArrayList<>(modIds);
        this.mc = mcVersion;
        this.callback = callback;
    }

    @Override
    public void run() {
        IDS.forEach(id -> {
            try {
                CurseForgeResponse resp = ModChecker.getForVersion(id.getModId(), mc);
                System.out.println(resp.getVersions());
                resp.getVersions().stream().min(Comparator.comparing(ModVersion::getUploadedAt)).filter(v -> v.getUploadedAt().isAfter(id.getUploadedAt())).ifPresent(updateables::add);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        });
        callback.call(updateables);
    }
}
