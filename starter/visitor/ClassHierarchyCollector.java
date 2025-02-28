package visitor;

import syntaxtree.*;
import java.util.*;

public class ClassHierarchyCollector extends DepthFirstVisitor {
    Map<String, Map<String, Set<String>>> classMethodVars = new HashMap<>();
    private String currentClass;
    private String currentMethod;
    private Set<String> currentVars = new HashSet<>(); // Initialize to avoid null pointer

    public Map<String, Map<String, Set<String>>> getClassMethodVars() {
        return classMethodVars;
    }

    @Override
    public void visit(ClassDeclaration n) {
        currentClass = n.f1.f0.tokenImage;
        classMethodVars.putIfAbsent(currentClass, new HashMap<>());
        super.visit(n);
    }

    @Override
    public void visit(MethodDeclaration n) {
        currentMethod = n.f2.f0.tokenImage;
        currentVars = new HashSet<>();
        classMethodVars.get(currentClass).put(currentMethod, currentVars);
        super.visit(n);
    }

    @Override
    public void visit(FormalParameter n) {
        if (n.f0.f0.choice instanceof IntegerType) {
            String varName = n.f1.f0.tokenImage;
            currentVars.add(varName);
            // System.out.println("Captured parameter: " + varName + " : int");
        }
    }

    @Override
    public void visit(VarDeclaration n) {
        if (n.f0.f0.choice instanceof IntegerType) {
            String varName = n.f1.f0.tokenImage;
            currentVars.add(varName);
            // System.out.println("Captured local variable: " + varName + " : int");
        }
    }
}
