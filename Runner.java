import java.util.List;

public class Runner {

	public static void main(String[] args) {
		try {
			//List<Token> results = LexicalAnalyser.analyse("public class foo { public static void main(String[] args){ int i = 0; if (i == 2) { i = i + 1; System.out.println(\"Hi\"); } else { i = i * 2; } } }");
			List<Token> results = LexicalAnalyser.analyse("public class Test { public static void main(String[] args){ if (true) {System.out.println(\"true\");} else { System.out.println(\"false\"); }}}");	
			System.out.println(results);
			ParseTree tree = SyntacticAnalyser.parse(results);
			System.out.println(tree);

			ParseTree result = SyntacticAnalyser.parse(LexicalAnalyser.analyse(
				"public class Test { public static void main(String[] args){ if (true) {System.out.println(\"true\");} else { System.out.println(\"false\"); }}}"));
		TreeNode ifNode = result.getRoot().getChildren().get(13).getChildren().get(0).getChildren().get(0);
			


		} catch (LexicalException e) {
			e.printStackTrace();
		} catch (SyntaxException e) {
			e.printStackTrace();
		}

	}

}
