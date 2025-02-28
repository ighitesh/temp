import syntaxtree.*;
import visitor.*;

import java.io.FileInputStream;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            new a2java(System.in);
            // Load the input program (you can replace this with an actual test case file)
            Node root = a2java.Goal();

            // Generate the CFG
            CFGGen cfgGen = new CFGGen();
            root.accept(cfgGen);
            ProgramCFG programCFG = cfgGen.getCFG();
            
            // Collect class and method information
            ClassHierarchyCollector classCollector = new ClassHierarchyCollector();
            root.accept(classCollector);
            Map<String, Map<String, Set<String>>> classMethodVars = classCollector.getClassMethodVars();
            
            // Run constant propagation
            ConstantPropagation cp = new ConstantPropagation();
            cp.runConstantPropagation(cfgGen, programCFG, classMethodVars);

            classCollector.printOutput();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

// You can create a sample TestProgram.java with a simple test case:
// class Test {
//     public static void main(String[] args) {
//         int a = 5;
//         int b = 10;
//         int c = a + b;
//         System.out.println(c);
//     }
// }
