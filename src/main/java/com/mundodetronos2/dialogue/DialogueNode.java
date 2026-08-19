package com.mundodetronos2.dialogue;

import java.util.ArrayList;
import java.util.List;

public class DialogueNode {
    private String id;
    private String text;
    private String sound;
    private List<DialogueOption> options = new ArrayList<>();

    public DialogueNode() {}

    public DialogueNode(String id, String text) {
        this.id = id;
        this.text = text;
    }

    public DialogueNode(String id, String text, String sound) {
        this.id = id;
        this.text = text;
        this.sound = sound;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getSound() { return sound; }
    public void setSound(String sound) { this.sound = sound; }

    public List<DialogueOption> getOptions() { return options; }
    public void setOptions(List<DialogueOption> options) { this.options = options; }

    public void addOption(DialogueOption option) {
        this.options.add(option);
    }
}
