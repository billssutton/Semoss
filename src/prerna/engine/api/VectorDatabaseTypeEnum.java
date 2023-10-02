package prerna.engine.api;

import prerna.util.sql.RdbmsTypeEnum;

public enum VectorDatabaseTypeEnum {

	FAISS("FAISS","prerna.engine.impl.vector.FaissDatabaseEngine");
	
	private String vectorDbName;
	private String vectorDbClass;
	private String vectorDbInitScript;
	
	VectorDatabaseTypeEnum(String vectorDbName, String vectorDbClass) {
		this.vectorDbName = vectorDbName;
		this.vectorDbClass = vectorDbClass;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getVectorDatabaseClass() {
		return this.vectorDbClass;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getVectorDatabaseName() {
		return this.vectorDbName;
	}
	
	/**
	 * 
	 * @param name
	 * @return
	 */
	public static VectorDatabaseTypeEnum getEnumFromName(String name) {
		VectorDatabaseTypeEnum[] allValues = values();
		for(VectorDatabaseTypeEnum v : allValues) {
			if(v.getVectorDatabaseName().equalsIgnoreCase(name)) {
				return v;
			}
		}
		throw new IllegalArgumentException("Invalid input for name " + name);
	}
	
	/**
	 * Get the enum from the driver
	 * @param driver
	 * @return
	 */
	public static VectorDatabaseTypeEnum getEnumFromClass(String vectorDbClass) {
		for(VectorDatabaseTypeEnum v : VectorDatabaseTypeEnum.values()) {
			if(vectorDbClass.equalsIgnoreCase(v.vectorDbClass)) {
				return v;
			}
		}
		return null;
	}
}
