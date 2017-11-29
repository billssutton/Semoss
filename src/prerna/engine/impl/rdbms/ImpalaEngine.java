package prerna.engine.impl.rdbms;

import prerna.engine.api.IEngine;
import prerna.query.interpreters.IQueryInterpreter2;
import prerna.query.interpreters.ImpalaInterpreter;

public class ImpalaEngine extends RDBMSNativeEngine {

	public ImpalaEngine() {
		
	}
	
	public IQueryInterpreter2 getQueryInterpreter2(){
		return new ImpalaInterpreter(this);
	}
	
	public ENGINE_TYPE getEngineType() {
		return IEngine.ENGINE_TYPE.IMPALA;
	}

}
