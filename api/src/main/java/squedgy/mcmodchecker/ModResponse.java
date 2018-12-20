package squedgy.mcmodchecker;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class ModResponse {

    private final List<ModVersion> versions = new LinkedList<>();
    private final List<ModMember> members = new LinkedList<>();

    public void addMember(ModMember member){ this.members.add(member); }

    public List<ModMember> getMembers(){ return new LinkedList<>(members); }

    public void addVersion(ModVersion version){ this.versions.add(version); }

    public List<ModVersion> getVersions(){ return new LinkedList<>(this.versions); }

    public List<ModVersion> getMatchingVersions(String version){
        return this.versions.stream()
                .filter(v -> v.getMinecraftVersion().equals(version))
                .collect(Collectors.toList());
    }

}
