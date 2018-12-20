package squedgy.mcmodchecker;

public class ModMember {

    private String title;
    private String username;

    public ModMember(String title, String username){
        this.title = title;
        this.username = username;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
