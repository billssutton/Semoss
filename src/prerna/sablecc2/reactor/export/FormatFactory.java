package prerna.sablecc2.reactor.export;

public class FormatFactory {

	public static Formatter getFormatter(String formatType) {
		
		switch(formatType.toUpperCase()) {
		
		case "TABLE": {
			return new TableFormatter();
		}
		
		case "GRAPH": {
			return new GraphFormatter();
		}
		
		case "JSON": {
			return new JsonFormatter();
		}
		
		case "KEYVALUE": {
			return new KeyValueFormatter();
		}
		
		default : {
			return new TableFormatter();
		}
		}
		
	}
}
