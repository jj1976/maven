package org.codehaus.mojo.javancss;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.maven.model.ReportPlugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.PathTool;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

/**
 * Generates a JavaNCSS report based on this module's source code.
 *
 * @goal report
 * @author <a href="jeanlaurentATgmail.com">Jean-Laurent de Morlhon</a>
 * @version $Id: NcssReportMojo.java 9906 2009-06-02 15:04:27Z jeanlaurent $
 */
public class NcssReportMojo
    extends AbstractMavenReport
{
    private static final String OUTPUT_NAME = "javancss";

    /**
     * Specifies the directory where the HTML report will be generated.
     *
     * @parameter expression="${project.reporting.outputDirectory}"
     * @required
     * @readonly
     */
    private File outputDirectory;

    /**
     * Specifies the directory where the XML report will be generated.
     *
     * @parameter default-value="${project.build.directory}"
     * @required
     */
    private File xmlOutputDirectory;

    /**
     * Specifies the maximum number of lines to take into account into the reports.
     *
     * @parameter default-value="30"
     * @required
     */
    private int lineThreshold;

    /**
     * Specified the name of the temporary file generated by JavaNCSS prior report generation.
     *
     * @parameter default-value="javancss-raw-report.xml"
     * @required
     */
    private String tempFileName;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter expression="${component.org.codehaus.doxia.site.renderer.SiteRenderer}"
     * @required
     * @readonly
     */
    private SiteRenderer siteRenderer;

    /**
     * The projects in the reactor for aggregation report.
     *
     * @parameter expression="${reactorProjects}"
     * @readonly
     */
    private List reactorProjects;

    /**
     * Link the violation line numbers to the source xref. Defaults to true and will link automatically if jxr plugin is
     * being used.
     *
     * @parameter expression="${linkXRef}" default-value="true"
     */
    private boolean linkXRef;

    /**
     * Location of the Xrefs to link to.
     *
     * @parameter default-value="${project.build.directory}/site/xref"
     */
    //private File xrefLocation;

    /**
     * List of ant-style patterns used to specify the java sources that should be included when running JavaNCSS. If
     * this is not specified, all .java files in the project source directories are included.
     *
     * @parameter
     */
    private String[] includes;

    /**
     * List of ant-style patterns used to specify the java sources that should be excluded when running JavaNCSS. If
     * this is not specified, no files in the project source directories are excluded.
     *
     * @parameter
     */
    private String[] excludes;

	private String relativeOutput;

	private String relativeXmlOutput;
    
    /**
     * @see org.apache.maven.reporting.MavenReport#executeReport(java.util.Locale)
     */
    public void executeReport( Locale locale )
        throws MavenReportException
    {
    	relativeOutput = 
    			PathTool.getRelativeFilePath(project.getBasedir().getAbsolutePath(), 
    					outputDirectory.getAbsolutePath());
    	
    	getLog().debug("relativeOutput: " + relativeOutput);
    	relativeXmlOutput = 
    			PathTool.getRelativeFilePath(project.getBasedir().getAbsolutePath(), 
    					xmlOutputDirectory.getAbsolutePath());
    	getLog().debug("relativeXmlOutput: " + relativeXmlOutput);
    	
    	if (project.getPackaging().equalsIgnoreCase("pom")) 
    	{
    		// it is an aggregated proyect
			LinkedList forLater = new LinkedList();
			for (Iterator i = reactorProjects.iterator(); i.hasNext();) {
				MavenProject mp = (MavenProject) i.next();
				ProjectReporter pr = new ProjectReporter(mp);

				if (pr.canGenerateSingleReport()) {
					getLog().debug("running");
					pr.generateJavaNcssXml(locale);
				}
				if (pr.canGenerateAggregateReport()) {
					forLater.addFirst(pr);
				}
			}
			for (Iterator i = forLater.iterator(); i.hasNext();) {
				ProjectReporter pr = (ProjectReporter) i.next();
				pr.generateAggregateReport(locale);
			}
		} 
    	else 
    	{
    		// it is a single project
    		ProjectReporter pr = new ProjectReporter(project);
    		if (pr.canGenerateSingleReport()) {
				pr.generateJavaNcssXml(locale);
				pr.generateSiteSingleReport(locale);
			}
//			if (pr.canGenerateAggregateReport()) {
//				pr.generateAggregateReport(locale);
//			}
    	}
    }
    
    // helper to retrieve the right bundle
    private static ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "javancss-report", locale, NcssReportMojo.class.getClassLoader() );
    }
    
    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.javancss.name" );
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.javancss.description" );
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    protected String getOutputDirectory()
    {
        return outputDirectory.getAbsolutePath();
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getProject()
     */
    protected MavenProject getProject()
    {
        return project;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getSiteRenderer()
     */
    protected SiteRenderer getSiteRenderer()
    {
        return siteRenderer;
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return OUTPUT_NAME;
    }
    
    private class ProjectReporter {
    	

    private MavenProject project;
    
    private File sourceDirectory;
   
    ProjectReporter(MavenProject mp){
    	project = mp;
    	sourceDirectory = new File (project.getBuild().getSourceDirectory());
    }
    

	private void generateAggregateReport( Locale locale )
        throws MavenReportException
    {
        // All this work just to get "target" so that we can scan the filesystem for
        // child javancss xml files...
        String basedir = project.getBasedir().toString();
        String output = getXmlOutputDirectory();
        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "basedir: " + basedir );
            getLog().debug( "output: " + output );
        }
        String relative = null;
        if ( output.startsWith( basedir ) )
        {
            relative = output.substring( basedir.length() + 1 );
        }
        else
        {
            getLog().error(
                            "Unable to aggregate report because I can't "
                                + "determine the relative location of the XML report" );
            return;
        }
        getLog().debug( "relative: " + relative );
        List reports = new ArrayList();
        for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
        {
            MavenProject child = (MavenProject) it.next();
            File xmlReport = new File( child.getBasedir() + File.separator + relative, tempFileName );
            if ( xmlReport.exists() )
            {
                reports.add( new ModuleReport( child, loadDocument( xmlReport ) ) );
            }
            else
            {
                getLog().debug( "xml file not found: " + xmlReport );
            }
        }
        getLog().debug( "Aggregating " + reports.size() + " JavaNCSS reports" );

        // parse the freshly generated file and write the report
        NcssAggregateReportGenerator reportGenerator =
            new NcssAggregateReportGenerator( getSink(), getBundle( locale ), getLog() );
        reportGenerator.doReport( locale, reports, lineThreshold );
    }

    private boolean isIncludeExcludeUsed()
    {
        return ( ( excludes != null ) || ( includes != null ) );
    }

    private void generateJavaNcssXml( Locale locale )
        throws MavenReportException
    {
    	
    	if (javaNcssXMLisUpToDate()){
    	/*
    	 * Check if the report already exists and if so check if it need to be generated again
    	 * by checking the sources and xml File.lastModified() times.
    	 */
    		getLog().debug("Skipping XML generation, already generated and up to date.");
    		return;
    	}
    	File outpuFile = new File(buildOutputFileName());
    	if (!outpuFile.getParentFile().exists())
    	{
    		outpuFile.getParentFile().mkdirs();
    	}
    	
        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "Calling NCSSExecuter with src    : " + sourceDirectory );
            getLog().debug( "Calling NCSSExecuter with output : " + buildOutputFileName() );
            getLog().debug( "Calling NCSSExecuter with includes : " + includes );
            getLog().debug( "Calling NCSSExecuter with excludes : " + excludes );
            getLog().debug( "Calling NCSSExecuter with encoding : " + getSourceEncoding() );
        }
        // run javaNCss and produce an temp xml file
        NcssExecuter ncssExecuter;
        if ( isIncludeExcludeUsed() )
        {
            ncssExecuter = new NcssExecuter( scanForSources(), buildOutputFileName() );
        }
        else
        {
            ncssExecuter = new NcssExecuter( sourceDirectory, buildOutputFileName() );
        }
        ncssExecuter.setEncoding( getSourceEncoding() ); // in case of null value, JavaNCSS uses platform encoding, as
        // expected

        ncssExecuter.execute();
        if ( !isTempReportGenerated() )
        {
            throw new MavenReportException( "Can't process temp ncss xml file." );
        }
    }
    
    private boolean javaNcssXMLisUpToDate() {
    	File xmlFile = new File(buildOutputFileName());
    	if (xmlFile.exists()){
    		File sourceLast = lastFileModified(sourceDirectory);
    		if (sourceLast == null) 
    		return (sourceLast != null)
    				&& (xmlFile.lastModified() > sourceLast.lastModified());
    	}
		return false;
	}

	private void generateSiteSingleReport(Locale locale) throws MavenReportException {
        // parse the freshly generated file and write the report
        NcssReportGenerator reportGenerator =
            new NcssReportGenerator( getSink(), getBundle( locale ), getLog(), constructXRefLocation() );
        reportGenerator.doReport( loadDocument(), lineThreshold );
    }

    /**
     * Load the xml file generated by javancss.
     */
    private Document loadDocument( File file )
        throws MavenReportException
    {
        try
        {
            SAXReader saxReader = new SAXReader();
            return saxReader.read( ReaderFactory.newXmlReader( file ) );
        }
        catch ( DocumentException de )
        {
            throw new MavenReportException( de.getMessage(), de );
        }
        catch ( IOException ioe )
        {
            throw new MavenReportException( ioe.getMessage(), ioe );
        }
    }

    private Document loadDocument()
        throws MavenReportException
    {
        return loadDocument( new File( buildOutputFileName() ) );
    }

    /**
     * Check that the expected temporary file generated by JavaNCSS exists.
     *
     * @return <code>true</code> if the temporary report exists, <code>false</code> otherwise.
     */
    private boolean isTempReportGenerated()
    {
        return new File( buildOutputFileName() ).exists();
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#canGenerateReport()
     */
//    public boolean canGenerateReport()
//    {
//        return ( canGenerateSingleReport() || canGenerateAggregateReport() );
//    }

    private boolean canGenerateAggregateReport()
    {
        if ( project.getModules().size() == 0 )
        {
            // no child modules
            return false;
        }
        if ( sourceDirectory != null && sourceDirectory.exists() )
        {
            // only non-source projects can aggregate
            String[] sources = scanForSources();
            return !( ( sources != null ) && ( sources.length > 0 ) );
        }
        return true;
    }

    private boolean canGenerateSingleReport()
    {
        if ( sourceDirectory == null || !sourceDirectory.exists() )
        {
        	getLog().debug("invalid source: " + sourceDirectory.getAbsolutePath());
            return false;
        }
        // now that we know we have a valid existing source directory
        // we check if any *.java files are existing.
        String[] sources = scanForSources();
        return ( sources != null ) && ( sources.length > 0 );
    }

    /**
     * gets a list of all files in the source directory.
     *
     * @return the list of all files in the source directory;
     */
    private String[] scanForSources()
    {
        String[] defaultIncludes = { "**\\*.java" };
        DirectoryScanner ds = new DirectoryScanner();
        if ( includes == null )
        {
            ds.setIncludes( defaultIncludes );
        }
        else
        {
            ds.setIncludes( includes );
        }
        if ( excludes != null )
        {
            ds.setExcludes( excludes );
        }
        ds.setBasedir( sourceDirectory );
        getLog().debug( "Scanning base directory " + sourceDirectory );
        ds.scan();
        int maxFiles = ds.getIncludedFiles().length;
        String[] result = new String[maxFiles];
        for ( int i = 0; i < maxFiles; i++ )
        {
            result[i] = sourceDirectory + File.separator + ds.getIncludedFiles()[i];
        }
        return result;
    }

    /**
     * Build a path for the output filename.
     *
     * @return A String representation of the output filename.
     */
    /* package */String buildOutputFileName()
    {
        return getXmlOutputDirectory() + File.separator + tempFileName;
    }
    
   /**
    * Gets the source files encoding.
    *
    * @return The source file encoding.
    */
   protected String getSourceEncoding()
   {
       return project.getProperties()
    		   .getProperty("project.build.sourceEncoding",
    				   Charset.defaultCharset().name());
   }

   protected String getOutputDirectory()
   {
       return project.getBasedir().getAbsolutePath() + File.separator + relativeOutput;
   }

   protected String getXmlOutputDirectory()
   {
	   return project.getBasedir().getAbsolutePath() + File.separator + relativeXmlOutput;
   }
   
    // blatantly copied from maven pmd plugin
    protected String constructXRefLocation()
    {
        String location = null;
        if ( linkXRef )
        {
        	File xrefLocation =  new File(getXmlOutputDirectory());
            String relativePath =
                PathTool.getRelativePath( getOutputDirectory(), xrefLocation.getAbsolutePath());
            if ( StringUtils.isEmpty( relativePath ) )
            {
                relativePath = project.getBasedir().getAbsolutePath();
            }
            relativePath = relativePath + "/" + xrefLocation.getName();
            if ( xrefLocation.exists() )
            {
                // XRef was already generated by manual execution of a lifecycle binding
                location = relativePath;
            }
            else
            {
                // Not yet generated - check if the report is on its way
                for ( Iterator reports = project.getReportPlugins().iterator(); reports.hasNext(); )
                {
                    ReportPlugin plugin = (ReportPlugin) reports.next();

                    String artifactId = plugin.getArtifactId();
                    if ( "maven-jxr-plugin".equals( artifactId ) || "jxr-maven-plugin".equals( artifactId ) )
                    {
                        location = relativePath;
                    }
                }
            }

            if ( location == null )
            {
                getLog().warn( "Unable to locate Source XRef to link to - DISABLED" );
            }
        }
        return location;
    }
    }
    
    public static File lastFileModified(File dir) {
    	File[] files = dir.listFiles();
    	long lastMod = Long.MIN_VALUE;
    	File choise = null;
    	for (int i = 0; i < files.length; i++) 
    	{
    		File file = files[i];
    		if (file.isDirectory()){
    			file = lastFileModified(file);
    		}
    		if (file != null
    				&& file.lastModified() > lastMod) {
    			choise = file;
    			lastMod = file.lastModified();
    		}
    	}
    	return choise;
    }
}