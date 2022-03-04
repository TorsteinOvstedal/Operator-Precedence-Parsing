import java.nio.charset.StandardCharsets;
import java.util.*;

// Basic implementation of the operator-precedence parsing algorithm.
// Source of algorithm, precedence table and test sentence:
// --> p. 69, https://www.vssut.ac.in/lecture_notes/lecture1422914957.pdf

// TODO
//  - Define the grammar the implementation handles.
//  - Testing.
//  - Comparison with other algorithms.
//  - Consider build a parse tree / an AST.
//  - Consider cleanup / refactoring.

public class Main {

    public static void main(String[] args){
        String expression = "(10 + 2.0 * 30) / 42.8 * 5 / 2 + 4 - 10 + 0";
        System.out.println(expression + " = " + eval(expression));

        expression = "0 / 0";
        System.out.println(expression + " = " + eval(expression));

        expression = "1 / 0";
        System.out.println(expression + " = " + eval(expression));

        // FIXME
        expression = "-1 / 0"; // Crash. Currently, no support for unary operators.
        System.out.println(expression + " = " + eval(expression));
    }

    public static Double eval(String expression) {
        Queue<Token> tokens = scan(expression.getBytes(StandardCharsets.UTF_8));
        tokens = new Parser().parse(tokens);

        Stack<Double> stack = new Stack<>();

        while (!tokens.isEmpty()) {
            Token token = tokens.poll();
            if (token.type() == Token.Type.NUMBER) {
                stack.push(Double.parseDouble(token.lexeme));
            } else {
                Double right = stack.pop();
                Double left = stack.pop();
                switch (token.type()) {
                    case ADD -> stack.push(left + right);
                    case SUB -> stack.push(left - right);
                    case MUL -> stack.push(left * right);
                    case DIV -> stack.push(left / right);
                }
            }
        }

        return stack.pop();
    }

    private static class Parser {

        private Token lookahead;
        private Queue<Token> tokens;
        private Stack<Token> stack;
        private Queue<Token> output;

        public synchronized Queue<Token> parse(Queue<Token> tokens) {
            this.tokens = tokens;
            this.stack = new Stack<>();
            this.stack.push(Token.END);
            this.output = new LinkedList<>();

            lookahead = tokens.poll();
            while (!(stack.peek().type() == Token.Type.END && lookahead.type() == Token.Type.END)) {
                switch (Precedence.relation(stack.peek(), lookahead)) {
                    case Precedence.L, Precedence.E -> shift();
                    case Precedence.G -> reduce();
                    default -> error();
                }
            }

            return output;
        }

        private void shift() {
            // System.out.println("[SHIFT]  " + lookahead);
            stack.push(lookahead);
            lookahead = tokens.poll();
        }

        private void reduce() {
            Token token;
            do {
                token = stack.pop();
                // System.out.println("[REDUCE] " + token);
                if (token.type() != Token.Type.LP && token.type() != Token.Type.RP)
                    output.add(token);
            } while (Precedence.relation(stack.peek(), token) != Precedence.L);
        }

        private void error() {
            throw new IllegalStateException();
        }
    }

    private static class Precedence {
        // Could have used chars directly in the table, but I found this easier to read
        public static final char N = ' '; // Error entry.
        public static final char L = '<'; // Less than.
        public static final char E = '='; // Equals to.
        public static final char G = '>'; // Greater than.

        // TODO: Check that ID corresponds to number in the table.
        private static final char[][] table = {
              // +  -  *  /  num (  )  END
                {G, G, L, L, L,  L, G, G}, // +
                {G, G, L, L, L,  L, G, G}, // -
                {G, G, G, G, L,  L, G, G}, // *
                {G, G, G, G, L,  L, G, G}, // /
                {G, G, G, G, N,  N, G, G}, // num
                {L, L, L, L, L,  L, E, N}, // LP
                {G, G, G, G, N,  N, G, G}, // RP
                {L, L, L, L, L,  L, N, N}  // EOS
        };

        private static int indexOf(Token.Type type) {
            return switch (type) {
                case ADD -> 0;
                case SUB -> 1;
                case MUL -> 2;
                case DIV -> 3;
                case NUMBER -> 4;
                case LP  -> 5;
                case RP  -> 6;
                case END -> 7;
            };
        }

        // Returns a < b || a = b || a > b || error.
        public static int relation(Token a, Token b) {
            return table[indexOf(a.type())][indexOf(b.type())];
        }
    }

    private record Token(Main.Token.Type type, String lexeme) {
        // FIXME: Storing lexeme in operators is currently redundant.
        //        Possible solution: Literal subclass (e.g. Number)
        //        where it makes sense to store the literal.

        public static final Token END = new Token(Type.END, null);  // EOS / EOF

        public enum Type {
            ADD, MUL, NUMBER, SUB, DIV, LP, RP, END
        }

        @Override public String toString() {
            return String.format("{%s, %s}", type.name(), lexeme);
        }
    }

    private static Queue<Token> scan(byte[] buffer) {
        Queue<Token> tokens = new LinkedList<>();

        int cursor = 0;
        while (cursor < buffer.length) {
            byte symbol = buffer[cursor++];
            switch (symbol) {
                case '+': tokens.add(new Token(Token.Type.ADD, "+")); break;
                case '-': tokens.add(new Token(Token.Type.SUB, "-")); break;
                case '*': tokens.add(new Token(Token.Type.MUL, "*")); break;
                case '/': tokens.add(new Token(Token.Type.DIV, "/")); break;
                case '(': tokens.add(new Token(Token.Type.LP, "(")); break;
                case ')': tokens.add(new Token(Token.Type.RP, ")")); break;
                case ' ':
                case '\t':
                case '\n':
                    break;
                default:
                    if (Character.isDigit(symbol)) {
                        int start = cursor - 1;
                        while (cursor < buffer.length && Character.isDigit(buffer[cursor]))
                            cursor++;
                        if (cursor < buffer.length && buffer[cursor] == '.') {
                            cursor++;
                            while (cursor < buffer.length && Character.isDigit(buffer[cursor]))
                                cursor++;
                        }
                        String lexeme = new String(buffer, start, cursor - start);
                        tokens.add(new Token(Token.Type.NUMBER, lexeme));
                    } else {
                        String message = String.format("Unexpected symbol '%c' at column %d.", symbol, cursor - 1);
                        throw new IllegalArgumentException(message);
                    }
            }
        }
        tokens.add(Token.END);

        return tokens;
    }
}
