package com.mundodetronos2.skills;

import java.util.HashMap;
import java.util.Map;

public class SkillTree {
    private final String roleId;
    private final Map<String, SkillNode> nodes = new HashMap<>();

    public SkillTree(String roleId) {
        this.roleId = roleId;
    }

    public String getRoleId() {
        return roleId;
    }

    public SkillTree addNode(SkillNode node) {
        this.nodes.put(node.getId().toLowerCase(), node);
        return this;
    }

    public SkillNode getNode(String id) {
        if (id == null) return null;
        return this.nodes.get(id.toLowerCase());
    }

    public Map<String, SkillNode> getNodes() {
        return nodes;
    }
}
