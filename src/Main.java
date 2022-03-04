import java.util.*;

// Basic implementation of the operator-precedence parsing algorithm.
// Source of algorithm, precedence table and test sentence:
// --> p. 69, https://www.vssut.ac.in/lecture_notes/lecture1422914957.pdf

// TODO
//  - Define which grammar the implementation handles.
//  - Build a parse tree.
//  - Testing.
//  - Comparison with other algorithms.
//  - Consider cleanup / refactoring.

public class Main {

    private static final Queue<Token> tokens = new LinkedList<>();
    static {
        tokens.add(Token.id("a"));
        tokens.add(Token.add());
        tokens.add(Token.id("b"));
        tokens.add(Token.mul());
        tokens.add(Token.id("c"));
        tokens.add(Token.eof());
    }

    public static void main(String[] args){
        new Parser().parse(tokens);
    }

    private static class Parser {

        Token lookahead;
        Queue<Token> tokens;
        Stack<Token> stack;

        public synchronized Stack<Token> parse(Queue<Token> tokens) {
            this.tokens = tokens;
            this.stack = new Stack<>();
            this.stack.push(Token.eof());

            lookahead = tokens.poll();
            while (!(stack.peek().type() == Token.Type.EOF && lookahead.type() == Token.Type.EOF)) {
                switch (Precedence.relation(stack.peek(), lookahead)) {
                    case Precedence.L, Precedence.E -> shift();
                    case Precedence.G -> reduce();
                    default -> error();
                }
            }

            accept();

            // TODO: Should return a parse tree.
            return null;
        }

        private void shift() {
            System.out.println("[SHIFT]  " + lookahead);
            stack.push(lookahead);
            lookahead = tokens.poll();
        }

        private void reduce() {
            Token token;
            do {
                token = stack.pop();
                System.out.println("[REDUCE] " + token);
            } while (Precedence.relation(stack.peek(), token) != Precedence.L);
        }

        private void accept() {
            System.out.println("[ACCEPT]");
        }

        private void error() {
            throw new IllegalStateException();
        }
    }

    private static class Precedence {
        public static final int N = -1; // Error entry.
        public static final int L =  0; // Less than.
        public static final int E =  1; // Equals to.
        public static final int G =  2; // Greater than.

        private static final int[][] table = {
              // +  -  *  /  id (  )  EOS
                {G, G, L, L, L, L, G, G}, // +
                {G, G, L, L, L, L, G, G}, // -
                {G, G, G, G, L, L, G, G}, // *
                {G, G, G, G, L, L, G, G}, // /
                {G, G, G, G, N, N, G, G}, // id
                {L, L, L, L, L, L, E, N}, // LP
                {G, G, G, G, N, N, G, G}, // RP
                {L, L, L, L, L, L, N, N}  // EOS
        };

        private static int indexOf(Token.Type type) {
            return switch (type) {
                case ADD -> 0;
                case SUB -> 1;
                case MUL -> 2;
                case DIV -> 3;
                case ID  -> 4;
                case LP  -> 5;
                case RP  -> 6;
                case EOF -> 7;
            };
        }

        // Returns a < b || a = b || a > b || error.
        public static int relation(Token a, Token b) {
            return table[indexOf(a.type())][indexOf(b.type())];
        }
    }

    private record Token(Main.Token.Type type, String lexeme) {
        public enum Type {ADD, MUL, ID, SUB, DIV, LP, RP, EOF}
        // Utility methods
        @Override public String toString()     { return String.format("[%s, %s]", type.name(), lexeme); }
        public static Token add()              { return new Token(Type.ADD, "+"); }
        public static Token mul()              { return new Token(Type.MUL, "*"); }
        public static Token id(String lexeme)  { return new Token(Type.ID, lexeme); }
        public static Token eof()              { return new Token(Type.EOF, null); }
    }
}
