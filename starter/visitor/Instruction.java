package visitor;

import syntaxtree.*;
import java.util.*;

public class Instruction {
    public Node instructionNode;
    Map<String, Integer> inSet;
    Map<String, Integer> outSet;

    public Instruction(Node instruction) {
        instructionNode = instruction;
        inSet = new HashMap<>();
        outSet = new HashMap<>();
    }

    public Map<String, Integer> getInSet() {
        return inSet;
    }

    public void setInSet(Map<String, Integer> inSet) {
        this.inSet = inSet;
    }

    public Map<String, Integer> getOutSet() {
        return outSet;
    }

    public void setOutSet(Map<String, Integer> outSet) {
        this.outSet = outSet;
    }
}