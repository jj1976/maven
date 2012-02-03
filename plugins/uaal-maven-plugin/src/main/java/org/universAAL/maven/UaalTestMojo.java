package org.universAAL.maven;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.universAAL.itests.conf.IntegrationTestConsts;
import org.universAAL.maven.treebuilder.ExecutionListCreator;

/**
 * This mojo creates composite file (artifact-test.composite) for project in
 * which it is executed. Additionally it resolves artifact specified in
 * IntegrationTest.RUN_DIR_MVN_URL which is needed by integration tests.
 * 
 * @goal test
 */
public class UaalTestMojo extends AbstractMojo {

    /**
     * @component
     * @required
     * @readonly
     */
    private ArtifactFactory artifactFactory;

    /**
     * @component
     * @required
     * @readonly
     */
    protected ArtifactResolver artifactResolver;

    /**
     * @component
     * @required
     * @readonly
     */
    private ArtifactMetadataSource artifactMetadataSource;

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter default-value="${ignore.dep.conflict}"
     * @readonly
     */
    private String throwExceptionOnConflictStr;

    /**
     * @component
     * @required
     * @readonly
     */
    private MavenProjectBuilder mavenProjectBuilder;

    /**
     * List of Remote Repositories used by the resolver
     * 
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    protected List remoteRepositories;

    /**
     * Location of the local repository.
     * 
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    protected ArtifactRepository localRepository;

    /**
     * @parameter default-value="${basedir}"
     * @readonly
     * @required
     */
    private File baseDirectory;

    public void execute() throws MojoExecutionException, MojoFailureException {
	try {
	    getLog()
		    .info(
			    System.getProperty("line.separator")
				    + System.getProperty("line.separator")
				    + "Creating composite file for itests - output generated in "
				    + IntegrationTestConsts.TEST_COMPOSITE
				    + System.getProperty("line.separator")
				    + System.getProperty("line.separator"));
	    ExecutionListCreator execListCreator = new ExecutionListCreator(
		    getLog(), artifactMetadataSource, artifactFactory,
		    mavenProjectBuilder, localRepository, remoteRepositories,
		    artifactResolver, throwExceptionOnConflictStr);
	    List mvnUrls = execListCreator.createArtifactExecutionList(project);
	    File targetDir = new File(baseDirectory, "target");
	    targetDir.mkdirs();
	    File generatedCompositeFile = new File(baseDirectory,
		    IntegrationTestConsts.TEST_COMPOSITE);
	    BufferedWriter compositeWriter = new BufferedWriter(
		    new OutputStreamWriter(new FileOutputStream(
			    generatedCompositeFile, false)));
	    for (Object mvnUrl : mvnUrls) {
		String mvnUrlStr = (String) mvnUrl;
		compositeWriter.write("scan-bundle:" + mvnUrlStr
			+ System.getProperty("line.separator"));
	    }
	    compositeWriter.close();
	    Artifact runDirArtifact = execListCreator
		    .parseMvnUrlWithType(IntegrationTestConsts.RUN_DIR_MVN_URL);
	    artifactResolver.resolve(runDirArtifact, remoteRepositories,
		    localRepository);
	} catch (Exception e) {
	    getLog().error(e);
	    throw new RuntimeException(e);
	}
    }
}