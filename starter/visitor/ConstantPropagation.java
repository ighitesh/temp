// Constant propagation with fixed-point iteration, handling only integer variables
package visitor;

import syntaxtree.*;
import java.util.*;

public class ConstantPropagation extends DepthFirstVisitor {
    public void runConstantPropagation(CFGGen cfgGen, ProgramCFG cfg, Map<String, Map<String, Set<String>>> classMethodVars) {
        boolean changed;
        int blockIndex = 0;
        
        do {
            changed = false;
    
            for (String method : cfg.methodBBSet.keySet()) {
                BB startBB = cfg.methodBBSet.get(method);
                Stack<BB> worklist = new Stack<>();
                worklist.push(startBB);
                Map<BB, Integer> blockIndices = new HashMap<>();
                Set<BB> visited = new HashSet<>();
    
                while (!worklist.isEmpty()) {
                    BB block = worklist.pop();
                    if (visited.contains(block)) continue;
                    visited.add(block);
                    blockIndices.putIfAbsent(block, blockIndex++);
    
                    // Save old OUT set
                    Map<String, Integer> oldOutSet = new HashMap<>(block.getOutSet());
    
                    // Compute IN set from predecessors
                    computeInSet(block);
    
                    String[] parts = method.split("_", 2);
                    String className = parts[0];
                    String methodName = parts[1];
    
                    Map<String, Set<String>> methods = new HashMap<>();
    
                    if (classMethodVars.containsKey(className)) {
                        methods = classMethodVars.get(className);
                    }                    
    
                    // Propagate constants through the block
                    propagateConstants(cfgGen, block, methods);
    
                    // Check for changes
                    if (!block.getOutSet().equals(oldOutSet)) {
                        changed = true;
                        worklist.addAll(block.outgoingEdges);
                    }
                }
            }
        } while (changed);  // Continue until no changes occur
    
        // **Printing happens here, after all propagation is complete**
        for (String method : cfg.methodBBSet.keySet()) {
            BB startBB = cfg.methodBBSet.get(method);
            System.out.println("Final CFG after propagation for method: " + method);
            printBlockInstructions(startBB);
        }
    }
    

    private void computeInSet(BB block) {
        Map<String, Integer> inSet = block.getInSet();
        inSet.clear();

        for (BB pred : block.incomingEdges) {
            Map<String, Integer> predOutSet = pred.getOutSet();
            for (String var : predOutSet.keySet()) {
                if (inSet.containsKey(var)) {
                    Integer existingVal = inSet.get(var);
                    Integer predVal = predOutSet.get(var);
                    if (existingVal == null || !existingVal.equals(predVal)) {
                        inSet.put(var, null); // Conflict => top (⊤)
                    }
                } else {
                    inSet.put(var, predOutSet.get(var));
                }
            }
        }
    }

    private void propagateConstants(CFGGen cfgGen, BB block, Map<String, Set<String>> methodVars) {
        Map<String, Integer> inSet = new HashMap<>(block.getInSet());
        Map<String, Integer> outSet = new HashMap<>(inSet);
        
        for (Instruction inst : block.instructions) {
            if (inst.instructionNode instanceof AssignmentStatement) {
                AssignmentStatement stmt = (AssignmentStatement) inst.instructionNode;
                String var = stmt.f0.f0.tokenImage;
                String expr = BB.getExpressionString(stmt.f2);
    
                // Debugging statement
                //System.out.println("Checking variable: " + var);
    
                boolean isIntVar = isIntegerVariable(var, methodVars);
                //System.out.println("  Detected as integer? " + isIntVar);
    
                if (isIntVar) {
                    Integer constVal = evaluateExpression(expr, inSet);
                    outSet.put(var, constVal);

                    // if (constVal != null) {
                    //     updateAST(cfgGen, block, var, constVal);
                    // }
    
                    // Print transformed assignment immediately
                    printAssignment(var, constVal, expr, inSet);
    
                    System.out.println("Propagating constant: " + var + " = " + (constVal != null ? constVal : "⊤"));
                }
    
                inst.setInSet(new HashMap<>(inSet));
                inst.setOutSet(new HashMap<>(outSet));
    
                inSet.putAll(outSet);
            } else {
                // Print non-assignment statements normally
                //System.out.println(getReadableInstruction(inst.instructionNode));
            }
        }
        
        block.getOutSet().clear();
        block.getOutSet().putAll(outSet);
    }

    private void updateAST(CFGGen cfgGen, BB block, String var, Integer constVal) {
        if (!cfgGen.blockToASTMap.containsKey(block)) return;
    
        for (Node node : cfgGen.blockToASTMap.get(block)) {
            if (node instanceof AssignmentStatement) {
                AssignmentStatement stmt = (AssignmentStatement) node;
                if (stmt.f0.f0.tokenImage.equals(var)) {
                    // Replace the right-hand side expression with the constant value
                    var temp = new NodeList(new PrimaryExpression(new NodeChoice(new IntegerLiteral(new NodeToken(constVal.toString())))));
                    stmt.f2 = new Expression(new NodeChoice(temp));
                }
            }
        }
    }
    
    
    // Function to print transformed assignment
    private void printAssignment(String var, Integer constVal, String originalExpr, Map<String, Integer> inSet) {
        String finalExpr;
        
        if (constVal != null) {
            finalExpr = constVal.toString(); // Fully replaced with constant
        } else {
            // Partially propagate known constants in the expression
            finalExpr = simplifyExpression(originalExpr, inSet);
        }
    
        System.out.println(var + " = " + finalExpr + ";");
    }    

    private Integer evaluateExpression(String expr, Map<String, Integer> inSet) {
        try {
            return Integer.parseInt(expr); // Direct constant
        } catch (NumberFormatException e) {
            // Handle binary expressions
            if (expr.contains("+")) {
                String[] parts = expr.split("\\+");
                Integer left = inSet.getOrDefault(parts[0].trim(), null);
                Integer right = inSet.getOrDefault(parts[1].trim(), null);
                return (left != null && right != null) ? left + right : null;
            }
            if (expr.contains("-")) {
                String[] parts = expr.split("-");
                Integer left = inSet.getOrDefault(parts[0].trim(), null);
                Integer right = inSet.getOrDefault(parts[1].trim(), null);
                return (left != null && right != null) ? left - right : null;
            }
            if (expr.contains("*")) {
                String[] parts = expr.split("\\*");
                Integer left = inSet.getOrDefault(parts[0].trim(), null);
                Integer right = inSet.getOrDefault(parts[1].trim(), null);
                return (left != null && right != null) ? left * right : null;
            }
            if (expr.contains("/")) {
                String[] parts = expr.split("/");
                Integer left = inSet.getOrDefault(parts[0].trim(), null);
                Integer right = inSet.getOrDefault(parts[1].trim(), null);
                return (left != null && right != null && right != 0) ? left / right : null;
            }
        }
        return inSet.getOrDefault(expr, null);
    }

    private boolean isIntegerVariable(String var, Map<String, Set<String>> methodVars) {
        for (String method : methodVars.keySet()) {
            // System.out.println("Method: " + method);
            // System.out.println("  Variables: " + methodVars.get(method));
    
            // Check if the variable exists in the current method's variable set
            if (methodVars.get(method).contains(var)) {
                // System.out.println("Found variable '" + var + "' in method '" + method + "'");
                return true;
            }
        }
    
        // System.out.println("Warning: Variable '" + var + "' not found as int in methodVars. Falling back to true.");
        return false;
    }    

    private void printBlockInstructions(BB block) {
        System.out.println("Simplified Code:");
        Map<String, Integer> outSet = block.getOutSet();
    
        for (Instruction inst : block.instructions) {
            Node node = inst.instructionNode;
            if (node instanceof AssignmentStatement) {
                AssignmentStatement stmt = (AssignmentStatement) node;
                String var = stmt.f0.f0.tokenImage;
                String expr = BB.getExpressionString(stmt.f2);
    
                // Replace variables with constants
                String simplifiedExpr = simplifyExpression(expr, outSet);
    
                // Print simplified assignment
                System.out.println(var + " = " + simplifiedExpr + ";");
    
            } else if (!(node instanceof VarDeclaration)) {
                // Print other statements as is, skip var declarations
                System.out.println(getReadableInstruction(node));
            }
        }
        System.out.println();
    }    
    
    // Helper to simplify expressions with constants
    private String simplifyExpression(String expr, Map<String, Integer> outSet) {
        Integer evaluatedValue = evaluateExpression(expr, outSet);
        
        if (evaluatedValue != null) {
            return evaluatedValue.toString(); // Print the final value if fully simplified
        }
    
        // Otherwise, just replace known variables and leave the rest
        String[] tokens = expr.split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            if (outSet.containsKey(tokens[i]) && outSet.get(tokens[i]) != null) {
                tokens[i] = outSet.get(tokens[i]).toString();
            }
        }
    
        return String.join(" ", tokens);
    }    

    private String getReadableInstruction(Node node) {
        if (node == null) {
            return "// NULL INSTRUCTION";
        }
        if (node instanceof AssignmentStatement) {
            AssignmentStatement stmt = (AssignmentStatement) node;
            return stmt.f0.f0.tokenImage + " = " + BB.getExpressionString(stmt.f2);
        }
        if (node instanceof VarDeclaration) {
            VarDeclaration varDecl = (VarDeclaration) node;
            if (varDecl.f0.f0.choice instanceof IntegerType) {
                return "int " + varDecl.f1.f0.tokenImage + ";";
            }
            else if (varDecl.f0.f0.choice instanceof BooleanType) {
                return "boolean " + varDecl.f1.f0.tokenImage + ";";
            }
            //return varDecl.f0.f0.choice.toString() + " " + varDecl.f1.f0.tokenImage + ";";
        }
        return node.toString();
    }    
}
