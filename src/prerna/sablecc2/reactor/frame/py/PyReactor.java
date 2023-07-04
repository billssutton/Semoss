package prerna.sablecc2.reactor.frame.py;

import java.util.List;
import java.util.Vector;

import org.apache.logging.log4j.Logger;

import prerna.algorithm.api.ICodeExecution;
import prerna.auth.utils.AbstractSecurityUtils;
import prerna.ds.py.PyTranslator;
import prerna.ds.py.PyUtils;
import prerna.om.Variable.LANGUAGE;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.util.Constants;
import prerna.util.DIHelper;
import prerna.util.Settings;
import prerna.util.Utility;

public class PyReactor extends AbstractPyFrameReactor implements ICodeExecution {
	
	private static transient SecurityManager defaultManager = System.getSecurityManager();
	private static final String CLASS_NAME = PyReactor.class.getName();
	// the code that was executed
	private String code = null;

	@Override
	public NounMetadata execute() {
		String disable_terminal =  DIHelper.getInstance().getProperty(Constants.DISABLE_TERMINAL);
		boolean nativePyServer = DIHelper.getInstance().getProperty(Settings.NATIVE_PY_SERVER) != null
				&& DIHelper.getInstance().getProperty(Settings.NATIVE_PY_SERVER).equalsIgnoreCase("true");

		if(disable_terminal != null && !disable_terminal.isEmpty() ) {
			if(Boolean.parseBoolean(disable_terminal)) {
				throw new IllegalArgumentException("Terminal and user code execution has been disabled.");
			}
		}
		
		if(!PyUtils.pyEnabled()) {
			throw new IllegalArgumentException("Python is not enabled to use the following command");
		}
		
		//check if py terminal is disabled
		String disable_py_terminal =  DIHelper.getInstance().getProperty(Constants.DISABLE_PY_TERMINAL);
		if(disable_py_terminal != null && !disable_py_terminal.isEmpty() ) {
			 if(Boolean.parseBoolean(disable_py_terminal)) {
					throw new IllegalArgumentException("Python terminal has been disabled.");
			 }
		}
		Logger logger = getLogger(CLASS_NAME);

		this.code = Utility.decodeURIComponent(this.curRow.get(0).toString());
		int tokens = code.split("\\n").length;

		PyTranslator pyTranslator = this.insight.getPyTranslator();
		pyTranslator.setLogger(logger);
		//String output = pyTranslator.runPyAndReturnOutput(code);
		String output = null;
		
		if(code.startsWith("sns.")) {
			return new NounMetadata("Please use PyPlot to plot your chart", PixelDataType.CONST_STRING);
		}
		
		if(AbstractSecurityUtils.securityEnabled()) {
			//if(tokens > 1) 
			{
				if(nativePyServer)
					output = pyTranslator.runScript(code) + "";
				else
					output = pyTranslator.runSingle(insight.getUser().getVarMap(), code, this.insight) + "";
			} 
			/*else {
				//output = pyTranslator.runScript(code) + "";
				output = pyTranslator.runScript(insight.getUser().getVarMap(), code) + "";
			}*/
		} else 
		{
			//if(tokens > 1) 
			{
				output = pyTranslator.runSingle(null, code, this.insight) + "";
			} 
			/*else {
				output = pyTranslator.runScript(code) + "";
			}*/
		}
		List<NounMetadata> outputs = new Vector<NounMetadata>(1);
		outputs.add(new NounMetadata(output, PixelDataType.CONST_STRING));
		
		boolean smartSync = (insight.getProperty("SMART_SYNC") != null) && insight.getProperty("SMART_SYNC").equalsIgnoreCase("true");
		//forcing smart sync to true
		smartSync = true;

		if(smartSync) {
			// if this returns true
			if(smartSync(pyTranslator)) {
				outputs.add(new NounMetadata(this.insight.getCurFrame(), PixelDataType.FRAME, PixelOperationType.FRAME_HEADERS_CHANGE));
			}
		}
		// call it here.. and if it return true
		// regenerate the metadata. 
		return new NounMetadata(outputs, PixelDataType.CODE, PixelOperationType.CODE_EXECUTION);
	}
	
	
	@Override
	public String getExecutedCode() {
		return this.code;
	}

	@Override
	public LANGUAGE getLanguage() {
		return LANGUAGE.PYTHON;
	}

	@Override
	public boolean isUserScript() {
		return true;
	}
}
