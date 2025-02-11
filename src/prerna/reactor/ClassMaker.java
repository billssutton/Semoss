package prerna.reactor;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import prerna.util.Constants;
import prerna.util.Utility;


public class ClassMaker {

	private static final Logger classLogger = LogManager.getLogger(ClassMaker.class);

	ClassPool pool = null;
	CtClass cc = null;
	boolean hasSuper = false;
	
	/**
	 * Generate the class
	 */
	public ClassMaker()
	{
		this("t" + Utility.getRandomString(12), "c" + Utility.getRandomString(12));
	}
	
	public ClassMaker(String packageName, String className)
	{
		pool = ClassPool.getDefault();
		pool.insertClassPath(new ClassClassPath(this.getClass())); 
		pool.insertClassPath(new ClassClassPath(prerna.util.Console.class)); 
		pool.importPackage("java.sql");
		pool.importPackage("java.lang");
		pool.importPackage("java.util");
		pool.importPackage("prerna.reactor");
		pool.importPackage("prerna.engine.api");
		pool.importPackage("prerna.util");
		pool.importPackage("org.apache.tinkerpop.gremlin.process.traversal");
		pool.importPackage("org.apache.tinkerpop.gremlin.structure");
		pool.importPackage("prerna.ds");
		//TODO: this one is for HeadersDataRow
		pool.importPackage("prerna.om");
		cc = pool.makeClass(packageName + ".c" + className); // the only reason I do this is if the user wants to do seomthing else
	}
	
	//sets the interface
	public void addInterface(String interfaceName)
	{
		try {
			CtClass[] interfaceVector = new CtClass[1];
			interfaceVector[0] = pool.getCtClass(interfaceName);
			cc.setInterfaces(interfaceVector);
		} catch (NotFoundException e) {
			classLogger.error(Constants.STACKTRACE, e);
		}
	}
	
	// add a super class
	public void addSuper(String superClassName)
	{
		if(!hasSuper)
		{
			try {
				cc.setSuperclass(pool.getCtClass(superClassName));
			} catch (NotFoundException e) {
				classLogger.error(Constants.STACKTRACE, e);
			} catch (CannotCompileException e) {
				classLogger.error(Constants.STACKTRACE, e);
			}
			hasSuper = true;
		}
	}
	
	// add a method
	public void addMethod(String daMethod)
	{
		try {
			cc.addMethod(CtNewMethod.make(daMethod, cc));
		} catch (CannotCompileException e) {
			classLogger.error(Constants.STACKTRACE, e);
		}
	}
	
	public void addField(String field) {
		try {
			cc.addField(CtField.make(field, cc));
		} catch (CannotCompileException e) {
			classLogger.error(Constants.STACKTRACE, e);
		}
	}
	
	public void addImport(String importPackage) {
		pool.importPackage(importPackage);
	}
	
	// gets the class back
	public Class toClass()
	{
		try {
			return cc.toClass();
		} catch (CannotCompileException e) {
			classLogger.error(Constants.STACKTRACE, e);
		}
		
		return null;
	}
	
	public void writeClass(String fileName)
	{
        try {
    		DataOutputStream out = new DataOutputStream(new FileOutputStream(fileName));
			cc.getClassFile().write(out);
		} catch (IOException e) {
			classLogger.error(Constants.STACKTRACE, e);
		}
	}
}
