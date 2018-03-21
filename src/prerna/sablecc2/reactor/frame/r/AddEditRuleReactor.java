package prerna.sablecc2.reactor.frame.r;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.codehaus.jackson.map.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.util.Utility;
import org.json.JSONObject;

public class AddEditRuleReactor extends AbstractRFrameReactor {

	/**
	 * This reactor runs the adds edit rules to the validateRulesTemplate.json
	 * The reactor takes as an input a map: 
	 * AddEditRule([{"testRule":{"rule":"<encode>grepl(\"^-\\\\d+$\", <x>) 
	 * 	== TRUE</encode>","columns":{"x":{"description":"the description for x","columnType":"STRING"}},
	 *  "description":"my description"}}]);
	 */
	
	public AddEditRuleReactor() {
		this.keysToGet = new String[] {ReactorKeysEnum.RULES_MAP.getKey()};
	}
	
	@Override
	public NounMetadata execute() {
		// initialize the rJava translator
		init();
		// grab the user input and get into map format
		List<Object> ruleList = this.curRow.getValuesOfType(PixelDataType.MAP);
		Map<String, Object> mapOptions = (Map<String, Object>) ruleList.get(0);

		// get the name of the rule
		// it is the value in the key set
		String name = "";
		Set<String> names = mapOptions.keySet();
		for (String item : names) {
			name = item;
		}

		// get input to the map
		HashMap<String, Object> mapOptionsEntry = (HashMap<String, Object>) mapOptions.get(name);
		// get the rule
		String rule = (String) mapOptionsEntry.get("rule");
		rule = Utility.decodeURIComponent(rule);
		mapOptionsEntry.replace("rule", rule);

		// create string builder for the r script
		StringBuilder rsb = new StringBuilder();

		// library validate
		rsb.append("library(validate);");

		// before we add to the json, we need to ensure that the rule is valid
		// in order to validate, we will need to first replace the <variableNames> with just the variableNames (remove the brackets)
		//first we have to get the columns from the column map
		String testRule = rule;
		Map<String, Object> columnMap = (Map<String, Object>) mapOptionsEntry.get("columns");
		for (String column : columnMap.keySet()) {
			// the unbracketed rule will be the test rule
			testRule = testRule.replaceAll("<" + column + ">", column);
		}
		
		// source the r function that will verify the rules validity
		String validateRuleScriptFilePath = getBaseFolder() + "\\R\\EditRules\\validateRule.R";
		validateRuleScriptFilePath = validateRuleScriptFilePath.replace("\\", "/");
		rsb.append("source(\"" + validateRuleScriptFilePath + "\");");

		// call the function in the r script and get the return value
		String rsbString = rsb.toString();
		this.rJavaTranslator.runR(rsbString);
        // script for running the function
		// when we validate, use the testRule
		String rScript = "as.character(validateRule(\"" + testRule + "\"))";
		// isValid will either be true or false and indicate if the rule is valid based on editrules in r
		String isValid = this.rJavaTranslator.getString(rScript);

		// only go forward with updating the json if the rule is valid
		if (isValid.equalsIgnoreCase("true")) {
			// if the rule is valid, then we add to the json
			// first read the existing json so that we can append
			String fileJsonPath = getBaseFolder() + "\\R\\EditRules\\validateRulesTemplate.json";
			String jsonString = "";

			HashMap<String, Object> editRulesTemplate = null;
			try {
				jsonString = new String(Files.readAllBytes(Paths.get(fileJsonPath)));
				editRulesTemplate = new ObjectMapper().readValue(jsonString, HashMap.class);
			} catch (IOException e2) {
				throw new IllegalArgumentException("Unable to read file from path: " + fileJsonPath);
			}
			
			// before adding, we should see if the name already exists in error rules template
			// if it does exist, we should throw an error and tell the user
			if (editRulesTemplate.keySet().contains(name)) {
				throw new IllegalArgumentException("The name of the rule already exists. Please use a different name for the rule.");
			}

			// we need to add our new map the the editRulesTemplate
			editRulesTemplate.put(name, mapOptionsEntry);

			// create the json string based on the map
			// use the GsonBuilder for proper formatting (and to avoid encoding) of the json
			GsonBuilder builder = new GsonBuilder();
			builder.disableHtmlEscaping();
			builder.setPrettyPrinting();
			Gson gson = builder.create();
			String json = gson.toJson(editRulesTemplate);

            // override the file with the new updated json
			PrintWriter pw;
			try {
				pw = new PrintWriter(new File(fileJsonPath));
				pw.write(json);
				pw.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
