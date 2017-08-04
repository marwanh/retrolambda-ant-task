/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 */

package net.marwanh.retrolambda.ant;

import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;

import net.orfjackal.retrolambda.Main;

public class RetrolambdaTask extends Task {

	private static final String RETROLAMBDA_QUIET = "retrolambda.quiet";
	private static final String RETROLAMBDA_INCLUDED_FILES_FILE = "retrolambda.includedFilesFile";
	private static final String RETROLAMBDA_INCLUDED_FILES = "retrolambda.includedFiles";
	private static final String RETROLAMBDA_CLASSPATH_FILE = "retrolambda.classpathFile";
	private static final String RETROLAMBDA_CLASSPATH = "retrolambda.classpath";
	private static final String RETROLAMBDA_INPUT_DIR = "retrolambda.inputDir";
	private static final String RETROLAMBDA_OUTPUT_DIR = "retrolambda.outputDir";
	private static final String RETROLAMBDA_DEFAULT_METHODS = "retrolambda.defaultMethods";
	private static final String RETROLAMBDA_BYTECODE_VERSION = "retrolambda.bytecodeVersion";

	@SuppressWarnings("serial")
	private Map<String, Object> retrolambdaProperties = new HashMap<String, Object>() {
		{
			put(RETROLAMBDA_BYTECODE_VERSION, 51);
			put(RETROLAMBDA_DEFAULT_METHODS, false);
			put(RETROLAMBDA_OUTPUT_DIR, null);
			put(RETROLAMBDA_INPUT_DIR, null); // required
			put(RETROLAMBDA_CLASSPATH, null); // required
			put(RETROLAMBDA_CLASSPATH_FILE, null);
			put(RETROLAMBDA_INCLUDED_FILES, null);
			put(RETROLAMBDA_INCLUDED_FILES_FILE, null);
			put(RETROLAMBDA_QUIET, false);
		}
	};

	private Project proj;

	@Override
	public void init() throws BuildException {
		super.init();
		proj = getProject();
		assert proj != null;
	}

	/* Attribute setters for Ant */
	public void setBytecodeversion(int bytecodeVersion) {
		retrolambdaProperties.put(RETROLAMBDA_BYTECODE_VERSION, bytecodeVersion);
	}

	public void setDefaultmethods(boolean defaultMethods) {
		retrolambdaProperties.put(RETROLAMBDA_DEFAULT_METHODS, defaultMethods);
	}

	public void setInputdir(File inputDir) {
		retrolambdaProperties.put(RETROLAMBDA_INPUT_DIR, inputDir);
	}

	public void setOutputdir(File outputDir) {
		retrolambdaProperties.put(RETROLAMBDA_OUTPUT_DIR, outputDir);
	}

	public void setClasspath(Path classpath) {
		addPathToProperty(classpath, RETROLAMBDA_CLASSPATH);
	}

	public void addClasspath(Path classpath) {
		addPathToProperty(classpath, RETROLAMBDA_CLASSPATH);
	}

	public void setClasspathfile(File classpathFile) {
		retrolambdaProperties.put(RETROLAMBDA_CLASSPATH_FILE, classpathFile);
	}

	public void setIncludedfiles(Path includedFiles) {
		addPathToProperty(includedFiles, RETROLAMBDA_INCLUDED_FILES);
	}

	public void addIncludedfiles(Path includedFiles) {
		addPathToProperty(includedFiles, RETROLAMBDA_INCLUDED_FILES);
	}

	public void setIncludedfilesfile(File includedFilesFile) {
		retrolambdaProperties.put(RETROLAMBDA_INCLUDED_FILES_FILE, includedFilesFile);
	}

	public void setQuiet(boolean quiet) {
		retrolambdaProperties.put(RETROLAMBDA_QUIET, quiet);
	}

	@Override
	public void execute() throws BuildException {
		checkRequiredAttributes();
		configureRetrolambda();

		Main.main(null);
	}

	private void configureRetrolambda() {
		Properties p = new Properties();
		for (String name : retrolambdaProperties.keySet()) {
			Object value = retrolambdaProperties.get(name);
			if (value != null)
				p.setProperty(name, value.toString());
		}
		p.list(System.out);
		p.putAll(System.getProperties());
		System.setProperties(p);
	}

	private void checkRequiredAttributes() throws BuildException {
		Object inputDir = retrolambdaProperties.get(RETROLAMBDA_INPUT_DIR);
		Object classpath = retrolambdaProperties.get(RETROLAMBDA_CLASSPATH);
		Object classpathFile = retrolambdaProperties.get(RETROLAMBDA_CLASSPATH_FILE);

		if (inputDir == null || (classpath == null && classpathFile == null))
			throw new BuildException("Attributes 'inputdir' and 'classpath' are both required.");
		/*
		 * if (java8home == null) throw new BuildException(
		 * "Attribute 'java8home' must contain the path to a Java 8 JDK/JRE.");
		 */
	}

	// Adds the path p to the property named propertyName.
	// propertyName must be one of RETROLAMBDA_INCLUDED_FILES, RETROLAMBDA_CLASSPATH
	private void addPathToProperty(Path p, String propertyName) {
		Path old = (Path) retrolambdaProperties.get(propertyName);
		Path result = new Path(getProject());
		result.append(old);
		result.append(p);
		retrolambdaProperties.put(propertyName, result);
	}
}
