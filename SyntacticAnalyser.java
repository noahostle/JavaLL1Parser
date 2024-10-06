import java.util.*;

public class SyntacticAnalyser {

	public static boolean DEBUG = false;

	public static ArrayList<TreeNode.Label> nullable = new ArrayList<>(List.of(TreeNode.Label.los, TreeNode.Label.forstart, TreeNode.Label.forarith, TreeNode.Label.elseifstat, TreeNode.Label.possif, TreeNode.Label.possassign, TreeNode.Label.boolexpr, TreeNode.Label.relexprprime, TreeNode.Label.arithexprprime, TreeNode.Label.termprime));

	public enum Control implements Symbol { endOfExpansion; @Override public boolean isVariable() {return false;} } //this is a control symbol that helps the logic know where each expansion ends, allowing it to move up one parent in the parsetree

	public static ParseTree parse(List<Token> tokens) throws SyntaxException {
		if (tokens.size() == 0) {
			throw new SyntaxException("Empty input is not allowed.");
		}
	
		HashMap<Pair<Symbol, Token.TokenType>, ArrayList<Symbol>> parseTable = initMap();	//init parsetable
		LinkedList<Symbol> stack = new LinkedList<Symbol>();								//init stack
		stack.push(TreeNode.Label.prog);													//add first variable to stack

		TreeNode parNode=new TreeNode(TreeNode.Label.prog, null);							//init parsetree
		ParseTree parseTree = new ParseTree(parNode);
		
		

		while (!tokens.isEmpty()){ 				//while the tokens list is not empty
			Symbol stackSymbol = stack.pop();	//pop the top stack symbol
			if (stackSymbol==Control.endOfExpansion) {parNode=parNode.getParent(); continue;} //if we have reached the end of the expansion, go to the parent
			if (stackSymbol.isVariable()){		//if the top stack symbol is a variable
				
				if (stackSymbol!=TreeNode.Label.prog){	//we dont want a duplicate root node
					TreeNode node = new TreeNode((TreeNode.Label)stackSymbol, parNode);;
					parNode.addChild(node);		//add current symbol to parsetree
					parNode=node;				//and select its node as parent for all the symbols we are about to expand from it
				}
			
				if (stackSymbol == TreeNode.Label.epsilon) {parNode=parNode.getParent();continue;}	//if we are on epsilon, go back to epsilons parent node, and continue (since we dont need to match epsilon to anything)
				
				Token inputSymbol = tokens.get(0);	//create a key for the parseTable hashmap using the top stack symbol and the left most input (without popping input token)
				Pair<Symbol, Token.TokenType> key = new Pair<Symbol, Token.TokenType>(stackSymbol, inputSymbol.getType());
				
				if (!parseTable.containsKey(key)){					//if theres no entry in the parseTable for our stack variable / input token combo...
					if (nullable.contains(stackSymbol)) continue;	//if the symbol is nullable, just move on (expand it to epsilon and then skip it)
					throw new SyntaxException("No rule found."); 	//otherwise, the input is not in the language		
				}
				
				ArrayList<Symbol> expansion = parseTable.get(key); 	//get the expansion corresponding to the rule from the parsetable
				stack.push(Control.endOfExpansion);					//place a control symbol at the end of the expansion, so we know when to move back up to the parent when creating the tree
				stack.addAll(0, expansion); 						//push the rule expansion in reverse order to stack
			} 
			
			
			else {												//if the top stack symbol is a terminal
				Token inputSymbol = tokens.remove(0);			//get the left-most token from input
				if (stackSymbol!=inputSymbol.getType()){break;}	//and they should be equal, if they arent then break out of loop. (if they are equal, stack/input have both been matched&popped, so continue)
				
				TreeNode node = new TreeNode(TreeNode.Label.terminal, inputSymbol, parNode);
				parNode.addChild(node);							//create a new node of the input token and add it to the current parent node
			}
		}												//as soon as input is empty (loop finishes), stack should only contain a control symbol, 
														//marking the end of the 'prog' expansion
		if (!(stack.get(0)==Control.endOfExpansion && stack.size()==1)){
			throw new SyntaxException("Not accepted.");	//so if this is not the case, the input isnt in the language.
		}
		return parseTree;
	}


	
public static HashMap<Double, ArrayList<Symbol>> initRules(){
		HashMap<Double, ArrayList<Symbol>> rules = new HashMap<>();
		//rule 1 <<prog>> → public class <<ID>> { public static void main ( String[] args ) { <<los>> } } 
		rules.put(1.0, new ArrayList<Symbol>(List.of(
			Token.TokenType.PUBLIC,
			Token.TokenType.CLASS,
			Token.TokenType.ID,
			Token.TokenType.LBRACE,
			Token.TokenType.PUBLIC,
			Token.TokenType.STATIC,
			Token.TokenType.VOID,
			Token.TokenType.MAIN,
			Token.TokenType.LPAREN,
			Token.TokenType.STRINGARR,
			Token.TokenType.ARGS,
			Token.TokenType.RPAREN,
			Token.TokenType.LBRACE,
			TreeNode.Label.los,
			Token.TokenType.RBRACE,
			Token.TokenType.RBRACE	
		)));
		//rule 2 <<los>> → <<stat>> <<los>> | ε
		rules.put(2.1,  new ArrayList<Symbol>(List.of(
			TreeNode.Label.stat,
			TreeNode.Label.los
		)));
		rules.put(2.2, new ArrayList<Symbol>(List.of(
			TreeNode.Label.epsilon
		)));
		//rule 3 <<stat>> → <<while>> | <<for>> | <<if>> | <<assign>> ; | <<decl>> ; | <<print>> ; | ;
		rules.put(3.1, new ArrayList<Symbol>(List.of(
			TreeNode.Label.whilestat
		)));
		rules.put(3.2, new ArrayList<Symbol>(List.of(
			TreeNode.Label.forstat
		)));
		rules.put(3.3, new ArrayList<Symbol>(List.of(
			TreeNode.Label.ifstat
		)));
		rules.put(3.4, new ArrayList<Symbol>(List.of(
			TreeNode.Label.assign,
			Token.TokenType.SEMICOLON
		)));
		rules.put(3.5, new ArrayList<Symbol>(List.of(
			TreeNode.Label.decl,
			Token.TokenType.SEMICOLON
		)));
		rules.put(3.6, new ArrayList<Symbol>(List.of(
			TreeNode.Label.print,
			Token.TokenType.SEMICOLON	
		)));
		rules.put(3.7, new ArrayList<Symbol>(List.of(
			Token.TokenType.SEMICOLON
		)));
		//rule 4 <<while>> → while ( <<rel expr>> <<bool expr>> ) { <<los>> } 
		rules.put(4.0, new ArrayList<Symbol>(List.of(
			Token.TokenType.WHILE,
			Token.TokenType.LPAREN,
			TreeNode.Label.relexpr,
			TreeNode.Label.boolexpr,
			Token.TokenType.RPAREN,
			Token.TokenType.LBRACE,
			TreeNode.Label.los,
			Token.TokenType.RBRACE
		)));
		//rule 5 <<for>> → for ( <<for start>> ; <<rel expr>> <<bool expr>> ; <<for arith>> ) { <<los>> } 
		rules.put(5.0, new ArrayList<Symbol>(List.of(
			Token.TokenType.FOR,
			Token.TokenType.LPAREN,
			TreeNode.Label.forstart,
			Token.TokenType.SEMICOLON,
			TreeNode.Label.relexpr,
			TreeNode.Label.boolexpr,
			Token.TokenType.SEMICOLON,
			TreeNode.Label.forarith,
			Token.TokenType.RPAREN,
			Token.TokenType.LBRACE,
			TreeNode.Label.los,
			Token.TokenType.RBRACE
		)));
		// Rule 6 <<for start>> → <<decl>> | <<assign>> | ε
		rules.put(6.1, new ArrayList<Symbol>(List.of(
			TreeNode.Label.decl
		)));
		rules.put(6.2, new ArrayList<Symbol>(List.of(
			TreeNode.Label.assign
		)));
		rules.put(6.3, new ArrayList<Symbol>(List.of(
			TreeNode.Label.epsilon
		)));
		//rule 7 <<for arith>> → <<arith expr>> | ε
		rules.put(7.1, new ArrayList<Symbol>(List.of(
			TreeNode.Label.arithexpr
		)));
		rules.put(7.2, new ArrayList<Symbol>(List.of(
			TreeNode.Label.epsilon
		)));
		//rule 8 <<if>> → if ( <<rel expr>> <<bool expr>> ) { <<los>> } <<else if>>
		rules.put(8.0, new ArrayList<Symbol>(List.of(
			Token.TokenType.IF,
			Token.TokenType.LPAREN,
			TreeNode.Label.relexpr,
			TreeNode.Label.boolexpr,
			Token.TokenType.RPAREN,
			Token.TokenType.LBRACE,
			TreeNode.Label.los,
			Token.TokenType.RBRACE,
			TreeNode.Label.elseifstat
		)));
		// Rule 9 <<else if>> → <<else?if>> { <<los>> } <<else if>> | ε
		rules.put(9.1, new ArrayList<Symbol>(List.of(
			TreeNode.Label.elseorelseif,
			Token.TokenType.LBRACE,
			TreeNode.Label.los,
			Token.TokenType.RBRACE,
			TreeNode.Label.elseifstat
		)));
		rules.put(9.2, new ArrayList<Symbol>(List.of(
			TreeNode.Label.epsilon
		)));
		//rule 10 <<else?if>> → else <<poss if>>
		rules.put(10.0, new ArrayList<Symbol>(List.of(
			Token.TokenType.ELSE,
			TreeNode.Label.possif
		)));
		//rule 11 <<poss if>> → if ( <<rel expr>> <<bool expr>> ) | ε
		rules.put(11.1, new ArrayList<Symbol>(List.of(
			Token.TokenType.IF,
			Token.TokenType.LPAREN,
			TreeNode.Label.relexpr,
			TreeNode.Label.boolexpr,
			Token.TokenType.RPAREN
		)));
		rules.put(11.2, new ArrayList<Symbol>(List.of(
			TreeNode.Label.epsilon
		)));
		//rule 12 <<assign>> → <<ID>> = <<expr>>
		rules.put(12.0, new ArrayList<Symbol>(List.of(
			Token.TokenType.ID,
			Token.TokenType.ASSIGN, //assign is '=', equal is '=='
			TreeNode.Label.expr
		)));
		//rule 13 <<decl>> → <<type>> <<ID>> <<poss assign>>
		rules.put(13.0, new ArrayList<Symbol>(List.of(
			TreeNode.Label.type,
			Token.TokenType.ID,
			TreeNode.Label.possassign
		)));
		//rule 14 <<poss assign>> → = <<expr>> | ε
		rules.put(14.1, new ArrayList<Symbol>(List.of(
			Token.TokenType.ASSIGN,
			TreeNode.Label.expr
		)));
		rules.put(14.2, new ArrayList<Symbol>(List.of(
			TreeNode.Label.epsilon
		)));
		// Rule 15 <<print>> → System.out.println ( <<print expr>> )
		rules.put(15.0, new ArrayList<Symbol>(List.of(
			Token.TokenType.PRINT,
			Token.TokenType.LPAREN,
			TreeNode.Label.printexpr,
			Token.TokenType.RPAREN
		)));
		//rule 16 <<type>> → int | boolean | char// we only need 1 rule because int, bool and char refer to 'TYPE' attribute in the enum
		rules.put(16.0, new ArrayList<Symbol>(List.of(
			Token.TokenType.TYPE 	//id
		)));
		//Rule 17 <<expr>> → <<rel expr>> <<bool expr>> | <<char expr>>
		rules.put(17.1, new ArrayList<Symbol>(List.of(
			TreeNode.Label.relexpr,
			TreeNode.Label.boolexpr
		)));
		rules.put(17.2, new ArrayList<Symbol>(List.of(
			TreeNode.Label.charexpr
		)));
		// Rule 18 <<char expr>> → ' <<char>> ' 
		rules.put(18.0, new ArrayList<Symbol>(List.of(
			Token.TokenType.SQUOTE,
			Token.TokenType.CHARLIT,
			Token.TokenType.SQUOTE
		)));
		//rule 19 <<bool expr>> → <<bool op>> <<rel expr>> <<bool expr>> | ε
		rules.put(19.1, new ArrayList<Symbol>(List.of(
			TreeNode.Label.boolop,
			TreeNode.Label.relexpr,
			TreeNode.Label.boolexpr
		)));
		rules.put(19.2, new ArrayList<Symbol>(List.of(
			TreeNode.Label.epsilon
		)));
		//rule 20 <<bool op>> → <<bool eq>> | <<bool log>>
		rules.put(20.1, new ArrayList<Symbol>(List.of(
			TreeNode.Label.booleq
		)));
		rules.put(20.2, new ArrayList<Symbol>(List.of(
			TreeNode.Label.boollog
		)));
		//rule 21 <<bool eq>> → == | != 
		rules.put(21.1, new ArrayList<Symbol>(List.of(
			Token.TokenType.EQUAL
		)));
		rules.put(21.2, new ArrayList<Symbol>(List.of(
			Token.TokenType.NEQUAL
		)));
		//rule 22 <<bool log>> → && | ||
		rules.put(22.1, new ArrayList<Symbol>(List.of(
			Token.TokenType.AND
		)));
		rules.put(22.2, new ArrayList<Symbol>(List.of(
			Token.TokenType.OR
		)));
		//rule 23 <<rel expr>> → <<arith expr>> <<rel expr'>> | true | false
		rules.put(23.1, new ArrayList<Symbol>(List.of(
			TreeNode.Label.arithexpr,
			TreeNode.Label.relexprprime
		)));
		rules.put(23.2, new ArrayList<Symbol>(List.of(
			Token.TokenType.TRUE
		)));
		rules.put(23.3, new ArrayList<Symbol>(List.of(
			Token.TokenType.FALSE
		)));
		//rule 24 <<rel expr'>> → <<rel op>> <<arith expr>> | ε
		rules.put(24.1, new ArrayList<Symbol>(List.of(
			TreeNode.Label.relop,
			TreeNode.Label.arithexpr
		)));
		rules.put(24.2, new ArrayList<Symbol>(List.of(
			TreeNode.Label.epsilon
		)));
		//rule 25 <<rel op>> → < | <= | > | >=
		rules.put(25.1, new ArrayList<Symbol>(List.of(
			Token.TokenType.LT
		)));
		rules.put(25.2, new ArrayList<Symbol>(List.of(
			Token.TokenType.LE
		)));
		rules.put(25.3, new ArrayList<Symbol>(List.of(
			Token.TokenType.GT
		)));
		rules.put(25.4, new ArrayList<Symbol>(List.of(
			Token.TokenType.GE
		)));
		//rule 26 <<arith expr>> → <<term>> <<arith expr'>>
		rules.put(26.0, new ArrayList<Symbol>(List.of(
			TreeNode.Label.term,
			TreeNode.Label.arithexprprime
		)));
		//rule 27 <<arith expr'>> → + <<term>> <<arith expr'>> | - <<term>> <<arith expr'>> | ε
		rules.put(27.1, new ArrayList<Symbol>(List.of(
			Token.TokenType.PLUS,
			TreeNode.Label.term,
			TreeNode.Label.arithexprprime
		)));
		rules.put(27.2, new ArrayList<Symbol>(List.of(
			Token.TokenType.MINUS,
			TreeNode.Label.term,
			TreeNode.Label.arithexprprime
		)));
		rules.put(27.3, new ArrayList<Symbol>(List.of(
			TreeNode.Label.epsilon
		)));
		//rule 28 <<term>> → <<factor>> <<term'>>
		rules.put(28.0, new ArrayList<Symbol>(List.of(
			TreeNode.Label.factor,
			TreeNode.Label.termprime
		)));
		//rule 29 <<term'>> → * <<factor>> <<term'>> | / <<factor>> <<term'>> | % <<factor>> <<term'>> | ε
		rules.put(29.1, new ArrayList<Symbol>(List.of(
			Token.TokenType.TIMES,
			TreeNode.Label.factor,
			TreeNode.Label.termprime
		)));
		rules.put(29.2, new ArrayList<Symbol>(List.of(
			Token.TokenType.DIVIDE,
			TreeNode.Label.factor,
			TreeNode.Label.termprime
		)));
		rules.put(29.3, new ArrayList<Symbol>(List.of(
			Token.TokenType.MOD,
			TreeNode.Label.factor,
			TreeNode.Label.termprime
		)));
		rules.put(29.4, new ArrayList<Symbol>(List.of(
			TreeNode.Label.epsilon
		)));
		//rule 30 <<factor>> → ( <<arith expr>> ) | <<ID>> | <<num>>
		rules.put(30.1, new ArrayList<Symbol>(List.of(
			Token.TokenType.LPAREN,
			TreeNode.Label.arithexpr,
			Token.TokenType.RPAREN
		)));
		rules.put(30.2, new ArrayList<Symbol>(List.of(
			Token.TokenType.ID
		)));
		rules.put(30.3, new ArrayList<Symbol>(List.of(
			Token.TokenType.NUM
		)));
		//rule 31 <<print expr>> → <<rel expr>> <<bool expr>> | " <<string lit>> "
		rules.put(31.1, new ArrayList<Symbol>(List.of(
			TreeNode.Label.relexpr,
			TreeNode.Label.boolexpr
		)));
		rules.put(31.2, new ArrayList<Symbol>(List.of(
			Token.TokenType.DQUOTE,
			Token.TokenType.STRINGLIT,
			Token.TokenType.DQUOTE
		)));
		return rules;
	}
	
	public static HashMap<Pair<Symbol, Token.TokenType>, ArrayList<Symbol>> initMap(){
		HashMap<Pair<Symbol, Token.TokenType>, ArrayList<Symbol>> parseTable = new HashMap<>();
		HashMap<Double, ArrayList<Symbol>> rules = initRules();

		//<<PROG>>
		parseTable.put(new Pair<>(TreeNode.Label.prog, Token.TokenType.PUBLIC), rules.get(1.0));
		//<<LOS>>
        parseTable.put(new Pair<>(TreeNode.Label.los, Token.TokenType.SEMICOLON), rules.get(2.1));
		parseTable.put(new Pair<>(TreeNode.Label.los, Token.TokenType.RBRACE), rules.get(2.2));
		parseTable.put(new Pair<>(TreeNode.Label.los, Token.TokenType.CHARLIT), rules.get(2.1));
		parseTable.put(new Pair<>(TreeNode.Label.los, Token.TokenType.PRINT), rules.get(2.1));
		parseTable.put(new Pair<>(TreeNode.Label.los, Token.TokenType.TYPE), rules.get(2.1));
		parseTable.put(new Pair<>(TreeNode.Label.los, Token.TokenType.WHILE), rules.get(2.1));
		parseTable.put(new Pair<>(TreeNode.Label.los, Token.TokenType.FOR), rules.get(2.1));
		parseTable.put(new Pair<>(TreeNode.Label.los, Token.TokenType.IF), rules.get(2.1));
		parseTable.put(new Pair<>(TreeNode.Label.los, Token.TokenType.ID), rules.get(2.1));
		parseTable.put(new Pair<>(TreeNode.Label.los, Token.TokenType.CHARLIT), rules.get(2.1));
		//<<STAT>>
		parseTable.put(new Pair<>(TreeNode.Label.stat, Token.TokenType.SEMICOLON), rules.get(3.7));
		parseTable.put(new Pair<>(TreeNode.Label.stat, Token.TokenType.PRINT), rules.get(3.6));
		parseTable.put(new Pair<>(TreeNode.Label.stat, Token.TokenType.TYPE), rules.get(3.5));
		parseTable.put(new Pair<>(TreeNode.Label.stat, Token.TokenType.WHILE), rules.get(3.1));
		parseTable.put(new Pair<>(TreeNode.Label.stat, Token.TokenType.ID), rules.get(3.4));
		parseTable.put(new Pair<>(TreeNode.Label.stat, Token.TokenType.FOR), rules.get(3.2));
		parseTable.put(new Pair<>(TreeNode.Label.stat, Token.TokenType.IF), rules.get(3.3));
		//<<WHILE>>
		parseTable.put(new Pair<>(TreeNode.Label.whilestat, Token.TokenType.WHILE), rules.get(4.0));
		//<<FOR>>
		parseTable.put(new Pair<>(TreeNode.Label.forstat, Token.TokenType.FOR), rules.get(5.0));
		//<<FORSTART>>
		parseTable.put(new Pair<>(TreeNode.Label.forstart, Token.TokenType.TYPE), rules.get(6.1));
		parseTable.put(new Pair<>(TreeNode.Label.forstart, Token.TokenType.ID), rules.get(6.2));
		parseTable.put(new Pair<>(TreeNode.Label.forstart, Token.TokenType.SEMICOLON), rules.get(6.3));
		//<<FORARITH>>
		parseTable.put(new Pair<>(TreeNode.Label.forarith, Token.TokenType.ID), rules.get(7.1));
		parseTable.put(new Pair<>(TreeNode.Label.forarith, Token.TokenType.NUM), rules.get(7.1));
		parseTable.put(new Pair<>(TreeNode.Label.forarith, Token.TokenType.LPAREN), rules.get(7.1));
		parseTable.put(new Pair<>(TreeNode.Label.forarith, Token.TokenType.RPAREN), rules.get(7.2));
		//<<IF>>
		parseTable.put(new Pair<>(TreeNode.Label.ifstat, Token.TokenType.IF), rules.get(8.0));
		//<<ELSEIF>>
		parseTable.put(new Pair<>(TreeNode.Label.elseifstat, Token.TokenType.ELSE), rules.get(9.1));
		parseTable.put(new Pair<>(TreeNode.Label.elseifstat, Token.TokenType.SEMICOLON), rules.get(9.2));
		parseTable.put(new Pair<>(TreeNode.Label.elseifstat, Token.TokenType.ID), rules.get(9.2));
		parseTable.put(new Pair<>(TreeNode.Label.elseifstat, Token.TokenType.PRINT), rules.get(9.2));
		parseTable.put(new Pair<>(TreeNode.Label.elseifstat, Token.TokenType.TYPE), rules.get(9.2));
		parseTable.put(new Pair<>(TreeNode.Label.elseifstat, Token.TokenType.WHILE), rules.get(9.2));
		parseTable.put(new Pair<>(TreeNode.Label.elseifstat, Token.TokenType.FOR), rules.get(9.2));
		parseTable.put(new Pair<>(TreeNode.Label.elseifstat, Token.TokenType.IF), rules.get(9.2));
		parseTable.put(new Pair<>(TreeNode.Label.elseifstat, Token.TokenType.RBRACE), rules.get(9.2));
		//<<Else?IF>>
		parseTable.put(new Pair<>(TreeNode.Label.elseorelseif, Token.TokenType.ELSE), rules.get(10.0));
		//<POSSIF>>
		parseTable.put(new Pair<>(TreeNode.Label.possif, Token.TokenType.IF), rules.get(11.1));
		parseTable.put(new Pair<>(TreeNode.Label.possif, Token.TokenType.LBRACE), rules.get(11.2));
		//<<ASSIGN>>
		parseTable.put(new Pair<>(TreeNode.Label.assign, Token.TokenType.ID), rules.get(12.0));
		//<<DECL>>
		parseTable.put(new Pair<>(TreeNode.Label.decl, Token.TokenType.TYPE), rules.get(13.0));
		//<<POSSASSIGN>>
		parseTable.put(new Pair<>(TreeNode.Label.possassign, Token.TokenType.ASSIGN), rules.get(14.1));
		parseTable.put(new Pair<>(TreeNode.Label.possassign, Token.TokenType.SEMICOLON), rules.get(14.2));
		//<<PRINT>>
		parseTable.put(new Pair<>(TreeNode.Label.print, Token.TokenType.PRINT), rules.get(15.0));
		//<<TYPE>>
		parseTable.put(new Pair<>(TreeNode.Label.type, Token.TokenType.TYPE), rules.get(16.0));
		//<<EXPR>>
		parseTable.put(new Pair<>(TreeNode.Label.expr, Token.TokenType.ID), rules.get(17.1));
		parseTable.put(new Pair<>(TreeNode.Label.expr, Token.TokenType.NUM), rules.get(17.1));
		parseTable.put(new Pair<>(TreeNode.Label.expr, Token.TokenType.LPAREN), rules.get(17.1));
		parseTable.put(new Pair<>(TreeNode.Label.expr, Token.TokenType.TRUE), rules.get(17.1));
		parseTable.put(new Pair<>(TreeNode.Label.expr, Token.TokenType.FALSE), rules.get(17.1));
		parseTable.put(new Pair<>(TreeNode.Label.expr, Token.TokenType.SQUOTE), rules.get(17.2));
		//<<CHAREXPR>>
		parseTable.put(new Pair<>(TreeNode.Label.charexpr, Token.TokenType.SQUOTE), rules.get(18.0));
		//<<BOOLEXPR>>
		parseTable.put(new Pair<>(TreeNode.Label.boolexpr, Token.TokenType.EQUAL), rules.get(19.1));
		parseTable.put(new Pair<>(TreeNode.Label.boolexpr, Token.TokenType.NEQUAL), rules.get(19.1));
		parseTable.put(new Pair<>(TreeNode.Label.boolexpr, Token.TokenType.AND), rules.get(19.1));
		parseTable.put(new Pair<>(TreeNode.Label.boolexpr, Token.TokenType.OR), rules.get(19.1));
		parseTable.put(new Pair<>(TreeNode.Label.boolexpr, Token.TokenType.ID), rules.get(19.2));
		parseTable.put(new Pair<>(TreeNode.Label.boolexpr, Token.TokenType.NUM), rules.get(19.2));
		parseTable.put(new Pair<>(TreeNode.Label.boolexpr, Token.TokenType.RPAREN), rules.get(19.2));
		parseTable.put(new Pair<>(TreeNode.Label.boolexpr, Token.TokenType.SEMICOLON), rules.get(19.2));
		//<<BOOLOP>>
		parseTable.put(new Pair<>(TreeNode.Label.boolop, Token.TokenType.EQUAL), rules.get(20.1));
		parseTable.put(new Pair<>(TreeNode.Label.boolop, Token.TokenType.NEQUAL), rules.get(20.1));
		parseTable.put(new Pair<>(TreeNode.Label.boolop, Token.TokenType.AND), rules.get(20.2));
		parseTable.put(new Pair<>(TreeNode.Label.boolop, Token.TokenType.OR), rules.get(20.2));
		//<<BOOLEQ>>
		parseTable.put(new Pair<>(TreeNode.Label.booleq, Token.TokenType.EQUAL), rules.get(21.1));
		parseTable.put(new Pair<>(TreeNode.Label.booleq, Token.TokenType.NEQUAL), rules.get(21.2));
		//<<BOOLLOG>>
		parseTable.put(new Pair<>(TreeNode.Label.boollog, Token.TokenType.AND), rules.get(22.1));
		parseTable.put(new Pair<>(TreeNode.Label.boollog, Token.TokenType.OR), rules.get(22.2));
		//<<RELEXPR>>
		parseTable.put(new Pair<>(TreeNode.Label.relexpr, Token.TokenType.ID), rules.get(23.1));
		parseTable.put(new Pair<>(TreeNode.Label.relexpr, Token.TokenType.NUM), rules.get(23.1));
		parseTable.put(new Pair<>(TreeNode.Label.relexpr, Token.TokenType.LPAREN), rules.get(23.1));
		parseTable.put(new Pair<>(TreeNode.Label.relexpr, Token.TokenType.TRUE), rules.get(23.2));
		parseTable.put(new Pair<>(TreeNode.Label.relexpr, Token.TokenType.FALSE), rules.get(23.3));
		//<<RELEXPRPRIME>>
		parseTable.put(new Pair<>(TreeNode.Label.relexprprime, Token.TokenType.LT), rules.get(24.1));
		parseTable.put(new Pair<>(TreeNode.Label.relexprprime, Token.TokenType.LE), rules.get(24.1));
		parseTable.put(new Pair<>(TreeNode.Label.relexprprime, Token.TokenType.GT), rules.get(24.1));
		parseTable.put(new Pair<>(TreeNode.Label.relexprprime, Token.TokenType.GE), rules.get(24.1));
		parseTable.put(new Pair<>(TreeNode.Label.relexprprime, Token.TokenType.SEMICOLON), rules.get(24.2));
		parseTable.put(new Pair<>(TreeNode.Label.relexprprime, Token.TokenType.RPAREN), rules.get(24.2));
		parseTable.put(new Pair<>(TreeNode.Label.relexprprime, Token.TokenType.EQUAL), rules.get(24.2));
		parseTable.put(new Pair<>(TreeNode.Label.relexprprime, Token.TokenType.NEQUAL), rules.get(24.2));
		parseTable.put(new Pair<>(TreeNode.Label.relexprprime, Token.TokenType.AND), rules.get(24.2));
		parseTable.put(new Pair<>(TreeNode.Label.relexprprime, Token.TokenType.OR), rules.get(24.2));
		//<<RELOP>>
		parseTable.put(new Pair<>(TreeNode.Label.relop, Token.TokenType.LT), rules.get(25.1));
		parseTable.put(new Pair<>(TreeNode.Label.relop, Token.TokenType.LE), rules.get(25.2));
		parseTable.put(new Pair<>(TreeNode.Label.relop, Token.TokenType.GT), rules.get(25.3));
		parseTable.put(new Pair<>(TreeNode.Label.relop, Token.TokenType.GE), rules.get(25.4));
		//<<ARITHEXPR>>
		parseTable.put(new Pair<>(TreeNode.Label.arithexpr, Token.TokenType.ID), rules.get(26.0));
		parseTable.put(new Pair<>(TreeNode.Label.arithexpr, Token.TokenType.NUM), rules.get(26.0));
		parseTable.put(new Pair<>(TreeNode.Label.arithexpr, Token.TokenType.LPAREN), rules.get(26.0));
		//<<ARITHEXPRPRIME>>
		parseTable.put(new Pair<>(TreeNode.Label.arithexprprime, Token.TokenType.PLUS), rules.get(27.1));
		parseTable.put(new Pair<>(TreeNode.Label.arithexprprime, Token.TokenType.MINUS), rules.get(27.2));
		parseTable.put(new Pair<>(TreeNode.Label.arithexprprime, Token.TokenType.SEMICOLON), rules.get(27.3));
		parseTable.put(new Pair<>(TreeNode.Label.arithexprprime, Token.TokenType.RPAREN), rules.get(27.3));
		parseTable.put(new Pair<>(TreeNode.Label.arithexprprime, Token.TokenType.EQUAL), rules.get(27.3));
		parseTable.put(new Pair<>(TreeNode.Label.arithexprprime, Token.TokenType.NEQUAL), rules.get(27.3));
		parseTable.put(new Pair<>(TreeNode.Label.arithexprprime, Token.TokenType.AND), rules.get(27.3));
		parseTable.put(new Pair<>(TreeNode.Label.arithexprprime, Token.TokenType.OR), rules.get(27.3));
		parseTable.put(new Pair<>(TreeNode.Label.arithexprprime, Token.TokenType.LT), rules.get(27.3));
		parseTable.put(new Pair<>(TreeNode.Label.arithexprprime, Token.TokenType.LE), rules.get(27.3));
		parseTable.put(new Pair<>(TreeNode.Label.arithexprprime, Token.TokenType.GT), rules.get(27.3));
		parseTable.put(new Pair<>(TreeNode.Label.arithexprprime, Token.TokenType.GE), rules.get(27.3));
		//<<TERM>>
		parseTable.put(new Pair<>(TreeNode.Label.term, Token.TokenType.ID), rules.get(28.0));
		parseTable.put(new Pair<>(TreeNode.Label.term, Token.TokenType.NUM), rules.get(28.0));
		parseTable.put(new Pair<>(TreeNode.Label.term, Token.TokenType.LPAREN), rules.get(28.0));
		//<<TERMPRIME>>
		parseTable.put(new Pair<>(TreeNode.Label.termprime, Token.TokenType.TIMES), rules.get(29.1));
		parseTable.put(new Pair<>(TreeNode.Label.termprime, Token.TokenType.DIVIDE), rules.get(29.2));
		parseTable.put(new Pair<>(TreeNode.Label.termprime, Token.TokenType.MOD), rules.get(29.3));
		parseTable.put(new Pair<>(TreeNode.Label.termprime, Token.TokenType.SEMICOLON), rules.get(29.4));
		parseTable.put(new Pair<>(TreeNode.Label.termprime, Token.TokenType.RPAREN), rules.get(29.4));
		parseTable.put(new Pair<>(TreeNode.Label.termprime, Token.TokenType.PLUS), rules.get(29.4));
		parseTable.put(new Pair<>(TreeNode.Label.termprime, Token.TokenType.MINUS), rules.get(29.4));
		parseTable.put(new Pair<>(TreeNode.Label.termprime, Token.TokenType.EQUAL), rules.get(29.4));
		parseTable.put(new Pair<>(TreeNode.Label.termprime, Token.TokenType.NEQUAL), rules.get(29.4));
		parseTable.put(new Pair<>(TreeNode.Label.termprime, Token.TokenType.AND), rules.get(29.4));
		parseTable.put(new Pair<>(TreeNode.Label.termprime, Token.TokenType.OR), rules.get(29.4));
		parseTable.put(new Pair<>(TreeNode.Label.termprime, Token.TokenType.LT), rules.get(29.4));
		parseTable.put(new Pair<>(TreeNode.Label.termprime, Token.TokenType.LE), rules.get(29.4));
		parseTable.put(new Pair<>(TreeNode.Label.termprime, Token.TokenType.GT), rules.get(29.4));
		parseTable.put(new Pair<>(TreeNode.Label.termprime, Token.TokenType.GE), rules.get(29.4));
		//<<FACTOR>>
		parseTable.put(new Pair<>(TreeNode.Label.factor, Token.TokenType.LPAREN), rules.get(30.1));
		parseTable.put(new Pair<>(TreeNode.Label.factor, Token.TokenType.ID), rules.get(30.2));
		parseTable.put(new Pair<>(TreeNode.Label.factor, Token.TokenType.NUM), rules.get(30.3));
		//<<PRINTEXPR>>
		parseTable.put(new Pair<>(TreeNode.Label.printexpr, Token.TokenType.ID), rules.get(31.1));
		parseTable.put(new Pair<>(TreeNode.Label.printexpr, Token.TokenType.NUM), rules.get(31.1));
		parseTable.put(new Pair<>(TreeNode.Label.printexpr, Token.TokenType.LPAREN), rules.get(31.1));
		parseTable.put(new Pair<>(TreeNode.Label.printexpr, Token.TokenType.TRUE), rules.get(31.1));
		parseTable.put(new Pair<>(TreeNode.Label.printexpr, Token.TokenType.FALSE), rules.get(31.1));
		parseTable.put(new Pair<>(TreeNode.Label.printexpr, Token.TokenType.DQUOTE), rules.get(31.2));
		
		//debug printing
		if (DEBUG){
			ArrayList<String> table = new ArrayList<String>();
			parseTable.forEach((key, value) -> table.add(String.format("%-" + 30 + "s", key) + "->		" + value));
			Collections.sort(table);
			table.forEach(System.out::println);
		}

		return parseTable;
	}

}
			
// The following class may be helpful.

class Pair<A, B> {
	private final A a;
	private final B b;

	public Pair(A a, B b) {
		this.a = a;
		this.b = b;
	}

	public A fst() {
		return a;
	}

	public B snd() {
		return b;
	}

	@Override
	public int hashCode() {
		return 3 * a.hashCode() + 7 * b.hashCode();
	}

	@Override
	public String toString() {
		return "{" + a + ", " + b + "}";
	}

	@Override
	public boolean equals(Object o) {
		if ((o instanceof Pair<?, ?>)) {
			Pair<?, ?> other = (Pair<?, ?>) o;
			return other.fst().equals(a) && other.snd().equals(b);
		}

		return false;
	}

}
