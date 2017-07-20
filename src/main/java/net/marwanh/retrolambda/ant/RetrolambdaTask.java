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

	Map<String, Object> retrolambdaProperties = new HashMap<String, Object>();

	private File retrolambdaJar;
	private File java8home;

	@Override
	public void init() throws BuildException {
		super.init();

		retrolambdaProperties.put("retrolambda.bytecodeVersion", 51);
		retrolambdaProperties.put("retrolambda.defaultMethods", false);
		retrolambdaProperties.put("retrolambda.inputDir", null); // required
		retrolambdaProperties.put("retrolambda.outputDir", null);
		retrolambdaProperties.put("retrolambda.classpath", null); // required
		retrolambdaProperties.put("retrolambda.classpathFile", null);
		retrolambdaProperties.put("retrolambda.includedFiles", null);
		retrolambdaProperties.put("retrolambda.includedFilesFile", null);
		retrolambdaProperties.put("retrolambda.quiet", false);
	}

	/* Attribute setters for Ant */
	public void setBytecodeversion(int bytecodeVersion) {
		retrolambdaProperties.put("retrolambda.bytecodeVersion", bytecodeVersion);
	}

	public void setDefaultmethods(boolean defaultMethods) {
		retrolambdaProperties.put("retrolambda.defaultMethods", defaultMethods);
	}

	public void setInputdir(File inputDir) {
		retrolambdaProperties.put("retrolambda.inputDir", inputDir);
	}

	public void setOutputdir(File outputDir) {
		retrolambdaProperties.put("retrolambda.outputDir", outputDir);
	}

	public void setClasspath(Path classpath) {
		retrolambdaProperties.put("retrolambda.classpath", classpath);
	}

	public void setClasspathfile(File classpathFile) {
		retrolambdaProperties.put("retrolambda.classpathFile", classpathFile);
	}

	public void setIncludedfiles(Path includedFiles) {
		retrolambdaProperties.put("retrolambda.includedFiles", includedFiles);
	}

	public void setIncludedfilesfile(File includedFilesFile) {
		retrolambdaProperties.put("retrolambda.includedFilesFile", includedFilesFile);
	}

	public void setQuiet(boolean quiet) {
		retrolambdaProperties.put("retrolambda.quiet", quiet);
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
		Object inputDir = retrolambdaProperties.get("retrolambda.inputDir");
		Object classpath = retrolambdaProperties.get("retrolambda.classpath");
		Object classpathFile = retrolambdaProperties.get("retrolambda.classpathFile");

		if (inputDir == null || (classpath == null && classpathFile == null))
			throw new BuildException("Attributes 'inputdir' and 'classpath' are both required.");
		if (retrolambdaJar == null)
			throw new BuildException("Attribute 'retrolambdajar' must contain the full path to the Retrolambda jar.");
		/*
		 * if (java8home == null) throw new BuildException(
		 * "Attribute 'java8home' must contain the path to a Java 8 JDK/JRE.");
		 */
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
		System.out.println(sb.toString());

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
