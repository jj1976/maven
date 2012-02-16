/*******************************************************************************
 * Copyright 2011 Universidad Politécnica de Madrid
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.universAAL.support.directives;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;

/**
 * @author amedrano
 * 
 * @goal svnIgnore-check
 * @phase process-sources
 */
public class SVNIgnoreCheckMojo extends AbstractMojo {

	private String NO_IGNORES = System.getProperty("line.separator")
			+ "\n"
			+ "SVN ignore Directive Fail :\n"
			+ "It seems the current Directory includes files that should be ingored.\n "
			+ "Remember to ignore these files with your svn client before you commit:\n";

	private String[] ignores = { ".project", ".settings", "target",
			".classpath", };
	/**
	 * @parameter expression="${failOnMissMatch}"
	 *            default-value="false"
	 */
	private boolean failOnMissMatch;
	/**
	 * @parameter expression="${directive.fix}" default-value="false"
	 */
	private boolean fixSCM;

	/** @parameter default-value="${project}" */
	private org.apache.maven.project.MavenProject mavenProject;

	public SVNIgnoreCheckMojo() {
		for (int i = 0; i < ignores.length; i++) {
			NO_IGNORES += ignores[i] + "\n";
		}
		NO_IGNORES += System.getProperty("line.separator");
	}

	/**
	 * {@inheritDoc}
	 */
	public void execute() throws MojoExecutionException, MojoFailureException {
		SVNClientManager cli = SVNClientManager.newInstance();
		this.getLog().debug(
				"checking svn ignore Rules in: "
						+ mavenProject.getBasedir().getPath());
		try {
			SVNWCClient wcCli = cli.getWCClient();
			SVNPropertyData pd = wcCli.doGetProperty(mavenProject.getBasedir(),
					SVNProperty.IGNORE, SVNRevision.WORKING,
					SVNRevision.WORKING);
			boolean changed = false;
			String prop = pd.getValue().getString();
			getLog().debug("Ignore Property contains: " + prop);// .split("\n")[0]
			for (int i = 0; i < ignores.length; i++) {
				if (!prop.contains(ignores[i]) && exists(ignores[i])) {
					prop += ignores[i] + "\n";
					changed = true;
				}
			}
			if (changed) {
				if (failOnMissMatch) {
					throw new MojoFailureException(NO_IGNORES);
				} else {
					getLog().warn(NO_IGNORES);
				}
				if (fixSCM) {

				}
			}
		} catch (SVNException e) {
			e.printStackTrace();
			getLog().warn("SVN Error.");
			getLog().warn("directory seems not to be a local SVN working copy.");
		}

	}

	private boolean exists(String string) {
		String[] files = mavenProject.getBasedir().list();
		int i = 0;
		getLog().debug("Matching: " + string);
		while (i < files.length && !files[i].endsWith(string.replace("*", ""))) {
			getLog().debug("No Match: " + files[i] + " with " + string);
			i++;
		}
		return files[i].endsWith(string.replace("*", ""));
	}

}
