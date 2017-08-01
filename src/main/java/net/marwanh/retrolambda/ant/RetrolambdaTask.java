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

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;

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
	Map<String, Object> retrolambdaProperties = new HashMap<String, Object>() {
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

	private File retrolambdaJar;
	private File java8home;

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
		addPath(classpath, RETROLAMBDA_CLASSPATH);
	}

	public void addClasspath(Path classpath) {
		addPath(classpath, RETROLAMBDA_CLASSPATH);
	}

	public void setClasspathfile(File classpathFile) {
		retrolambdaProperties.put(RETROLAMBDA_CLASSPATH_FILE, classpathFile);
	}

	public void setIncludedfiles(Path includedFiles) {
		addPath(includedFiles, RETROLAMBDA_INCLUDED_FILES);
	}

	public void addIncludedfiles(Path includedFiles) {
		addPath(includedFiles, RETROLAMBDA_INCLUDED_FILES);
	}

	public void setIncludedfilesfile(File includedFilesFile) {
		retrolambdaProperties.put(RETROLAMBDA_INCLUDED_FILES_FILE, includedFilesFile);
	}

	public void setQuiet(boolean quiet) {
		retrolambdaProperties.put(RETROLAMBDA_QUIET, quiet);
	}

	public void setRetrolambdajar(File retrolambdaJar) {
		this.retrolambdaJar = retrolambdaJar;
	}

	public void setJava8home(File java8home) {
		this.java8home = java8home;
	}

	@Override
	public void execute() throws BuildException {
		checkRequiredAttributes();
		List<String> cmd = getCommand();

		ProcessBuilder pb = new ProcessBuilder(cmd);
		int exit = 0;
		try {
			Process p = pb.start();
			exit = p.waitFor();
		} catch (IOException | InterruptedException ex) {
			throw new BuildException("Failed to run Retrolambda.");
		}
		if (exit != 0)
			throw new BuildException("Retrolambda returned a non-zero (" + exit + ") exit code.");
	}

	private void checkRequiredAttributes() throws BuildException {
		Object inputDir = retrolambdaProperties.get(RETROLAMBDA_INPUT_DIR);
		Object classpath = retrolambdaProperties.get(RETROLAMBDA_CLASSPATH);
		Object classpathFile = retrolambdaProperties.get(RETROLAMBDA_CLASSPATH_FILE);

		if (inputDir == null || (classpath == null && classpathFile == null))
			throw new BuildException("Attributes 'inputdir' and 'classpath' are both required.");
		if (retrolambdaJar == null)
			throw new BuildException("Attribute 'retrolambdajar' must contain the full path to the Retrolambda jar.");
		/*
		 * if (java8home == null) throw new BuildException(
		 * "Attribute 'java8home' must contain the path to a Java 8 JDK/JRE.");
		 */
	}

	// Adds the path p to the property named propertyName.
	// propertyName must be one of RETROLAMBDA_INCLUDED_FILES, RETROLAMBDA_CLASSPATH
	private void addPath(Path p, String propertyName) {
		Path old = (Path) retrolambdaProperties.get(propertyName);
		Path result = new Path(getProject());
		result.append(old);
		result.append(p);
		retrolambdaProperties.replace(propertyName, result);
	}

	private List<String> getCommand() {
		ArrayList<String> l = new ArrayList<>();

		String java = getJavaPath();
		if (java == null)
			throw new BuildException("Could not find the path to the Java command.");

		java = Paths.get(java, "bin/java").toString();
		l.add(java);
		for (String name : retrolambdaProperties.keySet()) {
			Object value = retrolambdaProperties.get(name);
			if (value != null) {
				String s = String.format("-D%s=%s", name, value);
				l.add(s);
			}
		}
		String s = retrolambdaJar.getPath();
		l.add(String.format("-javaagent:%s", s));
		l.add("-jar");
		l.add(s);

		StringBuilder sb = new StringBuilder();
		for (String string : l) {
			sb.append(string + " ");
		}

		return l;
	}

	private String getJavaPath() {
		String java = null;

		// if 'java8home' attribute is set, go with it
		if (java8home != null && java8home.exists()) {
			java = java8home.getPath();
		}
		// if not, look for an environment variable
		if (java == null || java.isEmpty())
			java = System.getenv("JAVA8_HOME");
		// Ultimately, fallback to the system's default java path
		if (java == null || java.isEmpty())
			java = System.getProperty("java.home");

		return java;
	}
}
