/*******************************************************************************
 * Indus, a program analysis and transformation toolkit for Java.
 * Copyright (c) 2001, 2007 Venkatesh Prasad Ranganath
 * 
 * All rights reserved.  This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 which accompanies 
 * the distribution containing this program, and is available at 
 * http://www.opensource.org/licenses/eclipse-1.0.php.
 * 
 * For questions about the license, copyright, and software, contact 
 * 	Venkatesh Prasad Ranganath at venkateshprasad.ranganath@gmail.com
 *                                 
 * This software was developed by Venkatesh Prasad Ranganath in SAnToS Laboratory 
 * at Kansas State University.
 *******************************************************************************/

package edu.ksu.cis.indus.xmlizer;

import edu.ksu.cis.indus.annotations.Empty;
import edu.ksu.cis.indus.common.soot.CompleteStmtGraphFactory;
import edu.ksu.cis.indus.common.soot.IStmtGraphFactory;
import edu.ksu.cis.indus.common.soot.NamedTag;
import edu.ksu.cis.indus.interfaces.IEnvironment;
import edu.ksu.cis.indus.processing.Environment;
import edu.ksu.cis.indus.processing.IProcessingFilter;
import edu.ksu.cis.indus.processing.OneAllStmtSequenceRetriever;
import edu.ksu.cis.indus.processing.ProcessingController;
import edu.ksu.cis.indus.processing.TagBasedProcessingFilter;

import java.io.File;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Scene;
import soot.SootClass;

/**
 * This utility class can be used to xmlize jimple.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.25 $ $Date: 2007/02/10 19:08:38 $
 */
public final class JimpleXMLizerCLI {

	/**
	 * The logger used by instances of this class to log messages.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(JimpleXMLizerCLI.class);

	// /CLOVER:OFF

	/**
	 * <i>This constructor cannot be used.</i>
	 */
	@Empty private JimpleXMLizerCLI() {
		// does nothing.
	}

	// /CLOVER:ON

	/**
	 * The entry point to execute this xmlizer from command prompt.
	 * 
	 * @param s is the command-line arguments.
	 * @throws RuntimeException when jimple xmlization fails.
	 * @pre s != null
	 */
	public static void main(final String[] s) {
		final Scene _scene = Scene.v();

		final Options _options = new Options();
		Option _o = new Option("d", "dump directory", true, "The directory in which to write the xml files.  "
				+ "If unspecified, the xml output will be directed standard out.");
		_o.setArgs(1);
		_o.setArgName("path");
		_o.setOptionalArg(false);
		_options.addOption(_o);
		_o = new Option("h", "help", false, "Display message.");
		_options.addOption(_o);
		_o = new Option("p", "soot-classpath", true, "Prepend this to soot class path.");
		_o.setArgs(1);
		_o.setArgName("classpath");
		_o.setOptionalArg(false);
		_options.addOption(_o);

		final HelpFormatter _help = new HelpFormatter();

		try {
			final CommandLine _cl = (new BasicParser()).parse(_options, s);
			final String[] _args = _cl.getArgs();

			if (_cl.hasOption('h')) {
				final String _cmdLineSyn = "java " + JimpleXMLizerCLI.class.getName() + "<options> <class names>";
				_help.printHelp(_cmdLineSyn.length(), _cmdLineSyn, "", _options, "", true);
			} else {
				if (_args.length > 0) {
					if (_cl.hasOption('p')) {
						_scene.setSootClassPath(_cl.getOptionValue('p') + File.pathSeparator + _scene.getSootClassPath());
					}

					final NamedTag _tag = new NamedTag("JimpleXMLizer");

					for (int _i = 0; _i < _args.length; _i++) {
						final SootClass _sc = _scene.loadClassAndSupport(_args[_i]);
						_sc.addTag(_tag);
					}
					final IProcessingFilter _filter = new TagBasedProcessingFilter(_tag.getName());
					writeJimpleAsXML(new Environment(_scene), _cl.getOptionValue('d'), null, new UniqueJimpleIDGenerator(),
							_filter);
				} else {
					System.out.println("No classes were specified.");
				}
			}
		} catch (final ParseException _e) {
			LOGGER.error("Error while parsing command line.", _e);
			printUsage(_options);
		} catch (final Throwable _e) {
			LOGGER.error("Beyond our control. May day! May day!", _e);
			throw new RuntimeException(_e);
		}
	}

	/**
	 * Writes the jimple in the scene via the writer.
	 * 
	 * @param env in which the jimple to be dumped resides.
	 * @param directory with which jimple is dumped. If <code>null</code>, the output will be redirected to standarad
	 *            output.
	 * @param suffix to be appended each file name. If <code>null</code>, no suffix is appended.
	 * @param jimpleIDGenerator is the id generator to be used during xmlization.
	 * @param processingFilter to be used to control the parts of the system that should be jimplified.
	 * @pre scene != null and jimpleIDGenerator != null
	 */
	public static void writeJimpleAsXML(final IEnvironment env, final String directory, final String suffix,
			final IJimpleIDGenerator jimpleIDGenerator, final IProcessingFilter processingFilter) {
		final JimpleXMLizer _xmlizer = new JimpleXMLizer(jimpleIDGenerator);
		final ProcessingController _pc = new ProcessingController();
		final IStmtGraphFactory<?> _sgf = new CompleteStmtGraphFactory();
		final OneAllStmtSequenceRetriever _ssr = new OneAllStmtSequenceRetriever();
		_ssr.setStmtGraphFactory(_sgf);
		_pc.setStmtSequencesRetriever(_ssr);
		_pc.setEnvironment(env);

		final XMLizingProcessingFilter _xmlFilter = new XMLizingProcessingFilter();

		if (processingFilter != null) {
			_xmlFilter.chain(processingFilter);
		}
		_pc.setProcessingFilter(_xmlFilter);
		_xmlizer.setDumpOptions(directory, suffix);
		_xmlizer.hookup(_pc);
		_pc.process();
		_xmlizer.unhook(_pc);
	}

	/**
	 * Prints the help/usage info for this class.
	 * 
	 * @param options is the command line option.
	 * @pre options != null
	 */
	private static void printUsage(final Options options) {
		final String _cmdLineSyn = "java " + JimpleXMLizerCLI.class.getName() + " <options> <classnames>";
		(new HelpFormatter()).printHelp(_cmdLineSyn, "Options are: ", options, "");
	}
}

// End of File
