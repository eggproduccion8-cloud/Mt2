package com.mundodetronos2.dialogue;

public class DialogueOption {
    private String text;
    private String nextNodeId;
    private String action; // "close", "accept_sacerdote", "complete_sacerdote", "accept_herrero", "complete_herrero", "claim_armor", "open_guide_book", "close_and_open_role_selection"

    public DialogueOption() {}

    public DialogueOption(String text, String nextNodeId, String action) {
        this.text = text;
        this.nextNodeId = nextNodeId;
        this.action = action;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getNextNodeId() { return nextNodeId; }
    public void setNextNodeId(String nextNodeId) { this.nextNodeId = nextNodeId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
