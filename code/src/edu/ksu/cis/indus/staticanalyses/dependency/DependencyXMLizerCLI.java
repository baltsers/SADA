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

package edu.ksu.cis.indus.staticanalyses.dependency;

import edu.ksu.cis.indus.common.collections.MapUtils;
import edu.ksu.cis.indus.common.collections.SetUtils;
import edu.ksu.cis.indus.common.datastructures.Pair.PairManager;
import edu.ksu.cis.indus.common.soot.BasicBlockGraphMgr;
import edu.ksu.cis.indus.common.soot.IStmtGraphFactory;
import edu.ksu.cis.indus.common.soot.MetricsProcessor;
import edu.ksu.cis.indus.common.soot.SootBasedDriver;
import edu.ksu.cis.indus.interfaces.ICallGraphInfo;
import edu.ksu.cis.indus.interfaces.IEnvironment;
import edu.ksu.cis.indus.interfaces.IEscapeInfo;
import edu.ksu.cis.indus.interfaces.IExceptionRaisingInfo;
import edu.ksu.cis.indus.interfaces.IMonitorInfo;
import edu.ksu.cis.indus.interfaces.IThreadGraphInfo;
import edu.ksu.cis.indus.interfaces.IUseDefInfo;
import edu.ksu.cis.indus.processing.IProcessor;
import edu.ksu.cis.indus.processing.OneAllStmtSequenceRetriever;
import edu.ksu.cis.indus.processing.ProcessingController;
import edu.ksu.cis.indus.processing.TagBasedProcessingFilter;
import edu.ksu.cis.indus.staticanalyses.callgraphs.CGBasedXMLizingProcessingFilter;
import edu.ksu.cis.indus.staticanalyses.callgraphs.CallGraphInfo;
import edu.ksu.cis.indus.staticanalyses.callgraphs.OFABasedCallInfoCollector;
import edu.ksu.cis.indus.staticanalyses.cfg.CFGAnalysis;
import edu.ksu.cis.indus.staticanalyses.cfg.ExceptionRaisingAnalysis;
import edu.ksu.cis.indus.staticanalyses.cfg.StaticFieldUseDefInfo;
import edu.ksu.cis.indus.staticanalyses.concurrency.MonitorAnalysis;
import edu.ksu.cis.indus.staticanalyses.concurrency.SafeLockAnalysis;
import edu.ksu.cis.indus.staticanalyses.concurrency.escape.EquivalenceClassBasedEscapeAnalysis;
import edu.ksu.cis.indus.staticanalyses.dependency.IDependencyAnalysis.DependenceSort;
import edu.ksu.cis.indus.staticanalyses.flow.instances.ofa.OFAnalyzer;
import edu.ksu.cis.indus.staticanalyses.flow.processors.AliasedUseDefInfo;
import edu.ksu.cis.indus.staticanalyses.flow.processors.AliasedUseDefInfov2;
import edu.ksu.cis.indus.staticanalyses.flow.processors.ThreadGraph;
import edu.ksu.cis.indus.staticanalyses.interfaces.IValueAnalyzer;
import edu.ksu.cis.indus.staticanalyses.processing.AnalysesController;
import edu.ksu.cis.indus.staticanalyses.processing.CGBasedProcessingFilter;
import edu.ksu.cis.indus.staticanalyses.processing.ValueAnalyzerBasedProcessingController;
import edu.ksu.cis.indus.staticanalyses.tokens.ITokens;
import edu.ksu.cis.indus.staticanalyses.tokens.TokenUtil;
import edu.ksu.cis.indus.staticanalyses.tokens.soot.SootValueTypeManager;
import edu.ksu.cis.indus.xmlizer.UniqueJimpleIDGenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Type;
import soot.Value;

/**
 * This class provides a command-line interface to xmlize dependence information. Refer to <code>SootBasedDriver</code> for
 * more configuration infomration.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.90 $ $Date: 2007/03/08 16:32:18 $
 */
public class DependencyXMLizerCLI
		extends SootBasedDriver {

	/**
	 * The logger used by instances of this class to log messages.
	 */
	public static final Logger LOGGER = LoggerFactory.getLogger(DependencyXMLizerCLI.class);

	/**
	 * This is the flow analyser used by the analyses being tested.
	 */
	protected IValueAnalyzer<Value> aa;

	/**
	 * A collection of dependence analyses.
	 */
	public List<IDependencyAnalysis> das = new ArrayList<IDependencyAnalysis>();

	/**
	 * This is a map from interface IDs to interface implementations that are required by the analyses being driven.
	 * 
	 * @invariant info.oclIsKindOf(Map(String, Object))
	 */
	protected final Map info = new HashMap();

	/**
	 * This indicates if common unchecked exceptions should be considered.
	 */
	public boolean commonUncheckedException;

	/**
	 * This flag indicates if jimple should be dumped.
	 */
	public boolean dumpJimple;

	/**
	 * This indicates if exceptional exits should be considered.
	 */
	public boolean exceptionalExits;

	/**
	 * This flag indicates if the simple version of aliased use-def information should be used.
	 */
	public boolean useAliasedUseDefv1;

	/**
	 * This indicates if safe lock should be used.
	 */
	public boolean useSafeLockAnalysis;

	/**
	 * The xmlizer used to xmlize dependence information.
	 */
	public final DependencyXMLizer xmlizer = new DependencyXMLizer();

	/**
	 * This is the entry point via command-line.
	 * 
	 * @param args is the command line arguments.
	 * @throws RuntimeException when an Throwable exception beyond our control occurs.
	 * @pre args != null
	 */
	public static void main(final String[] args) {
		final Options _options = new Options();
		Option _option = new Option("o", "output", true,
				"Directory into which xml files will be written into.  Defaults to current directory if omitted");
		_option.setArgs(1);
		_option.setArgName("output-directory");
		_options.addOption(_option);
		_option = new Option("j", "jimple", false, "Dump xmlized jimple.");
		_options.addOption(_option);

		final DivergenceDA _fidda = DivergenceDA.getDivergenceDA(IDependencyAnalysis.Direction.FORWARD_DIRECTION);
		_fidda.setConsiderCallSites(true);

		final DivergenceDA _bidda = DivergenceDA.getDivergenceDA(IDependencyAnalysis.Direction.BACKWARD_DIRECTION);
		_bidda.setConsiderCallSites(true);

		final NonTerminationSensitiveEntryControlDA _ncda = new NonTerminationSensitiveEntryControlDA();
		final Object[][] _dasOptions = {
				{"ibdda1", "Identifier based data dependence (Soot)", new IdentifierBasedDataDA()},
				{"ibdda2", "Identifier based data dependence (Indus)", new IdentifierBasedDataDAv2()},
				{"ibdda3", "Identifier based data dependence (Indus Optimized)", new IdentifierBasedDataDAv3()},
				{"rbdda", "Reference based data dependence", new ReferenceBasedDataDA()},
				{"nscda", "Non-termination sensitive Entry control dependence", _ncda},
				{"nicda", "Non-termination insensitive Entry control dependence",
						new NonTerminationInsensitiveEntryControlDA(),},
				{"xcda", "Exit control dependence", new ExitControlDA()},
				{"sda", "Synchronization dependence", new SynchronizationDA()},
				{"frda1", "Forward Ready dependence v1", ReadyDAv1.getForwardReadyDA()},
				{"brda1", "Backward Ready dependence v1", ReadyDAv1.getBackwardReadyDA()},
				{"frda2", "Forward Ready dependence v2", ReadyDAv2.getForwardReadyDA()},
				{"brda2", "Backward Ready dependence v2", ReadyDAv2.getBackwardReadyDA()},
				{"frda3", "Forward Ready dependence v3", ReadyDAv3.getForwardReadyDA()},
				{"brda3", "Backward Ready dependence v3", ReadyDAv3.getBackwardReadyDA()},
				{"ida1", "Interference dependence v1", new InterferenceDAv1()},
				{"ida2", "Interference dependence v2", new InterferenceDAv2()},
				{"ida3", "Interference dependence v3", new InterferenceDAv3()},
				{"fdda", "Forward Intraprocedural Divergence dependence",
						DivergenceDA.getDivergenceDA(IDependencyAnalysis.Direction.FORWARD_DIRECTION),},
				{"bdda", "Backward Intraprocedural Divergence dependence",
						DivergenceDA.getDivergenceDA(IDependencyAnalysis.Direction.BACKWARD_DIRECTION),},
				{"fidda", "Forward Intra+Interprocedural Divergence dependence", _fidda},
				{"bidda", "Backward Intra+Interprocedural Divergence dependence", _bidda},
				{"fpidda", "Forward Interprocedural Divergence dependence",
						InterProceduralDivergenceDA.getDivergenceDA(IDependencyAnalysis.Direction.FORWARD_DIRECTION),},
				{"bpidda", "Backward Interprocedural Divergence dependence",
						InterProceduralDivergenceDA.getDivergenceDA(IDependencyAnalysis.Direction.BACKWARD_DIRECTION),},};
		_option = new Option("h", "help", false, "Display message.");
		_option.setOptionalArg(false);
		_options.addOption(_option);
		_option = new Option("p", "soot-classpath", false, "Prepend this to soot class path.");
		_option.setArgs(1);
		_option.setArgName("classpath");
		_option.setOptionalArg(false);
		_options.addOption(_option);
		_option = new Option("aliasedusedefv1", false, "Use version 1 of aliased use-def info.");
		_options.addOption(_option);
		_option = new Option("safelockanalysis", false, "Use safe-lock-analysis for ready dependence.");
		_options.addOption(_option);
		_option = new Option("ofaforinterference", false, "Use OFA for interference dependence.");
		_options.addOption(_option);
		_option = new Option("ofaforready", false, "Use OFA for ready dependence.");
		_options.addOption(_option);
		_option = new Option("exceptionalexits", false, "Consider exceptional exits for control dependence.");
		_options.addOption(_option);
		_option = new Option("commonuncheckedexceptions", false, "Consider common unchecked exceptions.");
		_options.addOption(_option);
		_option = new Option("S", "scope", true, "The scope that should be analyzed.");
		_option.setArgs(1);
		_option.setArgName("scope");
		_option.setRequired(false);
		_options.addOption(_option);

		for (int _i = 0; _i < _dasOptions.length; _i++) {
			final String _shortOption = _dasOptions[_i][0].toString();
			final String _description = _dasOptions[_i][1].toString();
			_option = new Option(_shortOption, false, _description);
			_options.addOption(_option);
		}

		final CommandLineParser _parser = new GnuParser();

		try {
			final CommandLine _cl = _parser.parse(_options, args);

			if (_cl.hasOption("h")) {
				printUsage(_options);
				System.exit(1);
			}

			final DependencyXMLizerCLI _xmlizerCLI = new DependencyXMLizerCLI();
			String _outputDir = _cl.getOptionValue('o');

			if (_outputDir == null) {
				if (LOGGER.isWarnEnabled()) {
					LOGGER.warn("Defaulting to current directory for output.");
				}
				_outputDir = ".";
			}

			_xmlizerCLI.xmlizer.setXmlOutputDir(_outputDir);

			if (_cl.hasOption('p')) {
				_xmlizerCLI.addToSootClassPath(_cl.getOptionValue('p'));
			}

			if (_cl.hasOption('S')) {
				_xmlizerCLI.setScopeSpecFile(_cl.getOptionValue('S'));
			}

			_xmlizerCLI.dumpJimple = _cl.hasOption('j');
			_xmlizerCLI.useAliasedUseDefv1 = _cl.hasOption("aliasedusedefv1");
			_xmlizerCLI.useSafeLockAnalysis = _cl.hasOption("safelockanalysis");
			_xmlizerCLI.exceptionalExits = _cl.hasOption("exceptionalexits");
			_xmlizerCLI.commonUncheckedException = _cl.hasOption("commonuncheckedexceptions");

			final List<String> _classNames = _cl.getArgList();

			if (_classNames.isEmpty()) {
				throw new MissingArgumentException("Please specify atleast one class.");
			}
			_xmlizerCLI.setClassNames(_classNames);

			final int _exitControlDAIndex = 6;

			if (_cl.hasOption(_dasOptions[_exitControlDAIndex][0].toString())) {
				_xmlizerCLI.das.add(_ncda);

				for (final Iterator<DependenceSort> _i = _ncda.getIds().iterator(); _i.hasNext();) {
					final DependenceSort _id = _i.next();
					MapUtils.putIntoCollectionInMapUsingFactory(_xmlizerCLI.info, _id, _ncda, SetUtils.getFactory());
				}
			}

			if (!parseForDependenceOptions(_dasOptions, _cl, _xmlizerCLI)) {
				throw new ParseException("Atleast one dependence analysis must be requested.");
			}

			_xmlizerCLI.<ITokens> execute();
		} catch (final ParseException _e) {
			LOGGER.error("Error while parsing command line.", _e);
			System.out.println("Error while parsing command line." + _e);
			printUsage(_options);
		} catch (final Throwable _e) {
			LOGGER.error("Beyond our control. May day! May day!", _e);
			throw new RuntimeException(_e);
		}
	}

	/**
	 * Parses command line for dependence analysis options.
	 * 
	 * @param dependenceOptions supported by this CLI.
	 * @param cmdLine provided by the user.
	 * @param xmlizerCLI that will be influenced by the provided dependence analysis options.
	 * @return <code>false</code> if no dependence options were parsed; <code>true</code>, otherwise.
	 */
	public static boolean parseForDependenceOptions(final Object[][] dependenceOptions, final CommandLine cmdLine,
			final DependencyXMLizerCLI xmlizerCLI) {
		boolean _flag = false;

		for (int _i = 0; _i < dependenceOptions.length; _i++) {
			if (cmdLine.hasOption(dependenceOptions[_i][0].toString())) {
				final IDependencyAnalysis _da = (IDependencyAnalysis) dependenceOptions[_i][2];
				xmlizerCLI.das.add(_da);
				_flag = true;

				if (_da instanceof InterferenceDAv1) {
					((InterferenceDAv1) _da).setUseOFA(cmdLine.hasOption("ofaforinterference"));
				}

				if (_da instanceof ReadyDAv1) {
					((ReadyDAv1) _da).setUseOFA(cmdLine.hasOption("ofaforready"));
					((ReadyDAv1) _da).setUseSafeLockAnalysis(xmlizerCLI.useSafeLockAnalysis);
				}
			}
		}
		return _flag;
	}

	public static boolean parseForDependenceOptions(final Object[][] dependenceOptions, DependencyXMLizerCLI xmlizerCLI) {
		boolean _flag = false;

		for (int _i = 0; _i < dependenceOptions.length; _i++) {
			{
				final IDependencyAnalysis _da = (IDependencyAnalysis) dependenceOptions[_i][2];
				xmlizerCLI.das.add(_da);
				_flag = true;

				if (_da instanceof InterferenceDAv1) {
					((InterferenceDAv1) _da).setUseOFA(false);
				}

				if (_da instanceof ReadyDAv1) {
					((ReadyDAv1) _da).setUseOFA(false);
					((ReadyDAv1) _da).setUseSafeLockAnalysis(false);
				}
			}
		}
		return _flag;
	}
	/**
	 * Prints the help/usage info for this class.
	 * 
	 * @param options is the command line option.
	 * @pre options != null
	 */
	public static void printUsage(final Options options) {
		final String _cmdLineSyn = "java " + DependencyXMLizerCLI.class.getName() + " <options> <classnames>";
		(new HelpFormatter()).printHelp(_cmdLineSyn, "Options are: ", options, "");
	}

	/**
	 * Drives the analyses.
	 * 
	 * @param <T> dummy type parameter.
	 */
	public <T extends ITokens<T, Value>> void execute() {
		setInfoLogger(LOGGER);

		final String _tagName = "DependencyXMLizer:FA";
		aa = OFAnalyzer.getFSOSAnalyzer(_tagName, TokenUtil.<T, Value, Type> getTokenManager(new SootValueTypeManager()),
				getStmtGraphFactory());

		final ValueAnalyzerBasedProcessingController _pc = new ValueAnalyzerBasedProcessingController();
		final Collection<IProcessor> _processors = new ArrayList<IProcessor>();
		final PairManager _pairManager = new PairManager(false, true);
		final CallGraphInfo _cgi = new CallGraphInfo(new PairManager(false, true));
		final IThreadGraphInfo _tgi = new ThreadGraph(_cgi, new CFGAnalysis(_cgi, getBbm()), _pairManager);
		//final IExceptionRaisingInfo _eti = new ExceptionRaisingAnalysis(getStmtGraphFactory(), _cgi, aa.getEnvironment());
		final ProcessingController _xmlcgipc = new ProcessingController();
		final ValueAnalyzerBasedProcessingController _cgipc = new ValueAnalyzerBasedProcessingController();
		final MetricsProcessor _countingProcessor = new MetricsProcessor();
		final OFABasedCallInfoCollector _callGraphInfoCollector = new OFABasedCallInfoCollector();
		final OneAllStmtSequenceRetriever _ssr = new OneAllStmtSequenceRetriever();
		_ssr.setStmtGraphFactory(getStmtGraphFactory());
		_pc.setStmtSequencesRetriever(_ssr);
		_pc.setAnalyzer(aa);
		_pc.setProcessingFilter(new TagBasedProcessingFilter(_tagName));

		_cgipc.setAnalyzer(aa);
		_cgipc.setProcessingFilter(new CGBasedProcessingFilter(_cgi));
		_cgipc.setStmtSequencesRetriever(_ssr);

		_xmlcgipc.setEnvironment(aa.getEnvironment());
		_xmlcgipc.setProcessingFilter(new CGBasedXMLizingProcessingFilter(_cgi));
		_xmlcgipc.setStmtSequencesRetriever(_ssr);

		final StaticFieldUseDefInfo _staticFieldUD = new StaticFieldUseDefInfo();
		final AliasedUseDefInfo _aliasUD;

		if (useAliasedUseDefv1) {
			_aliasUD = new AliasedUseDefInfo(aa, bbm, _pairManager, new CFGAnalysis(_cgi, bbm));
		} else {
			_aliasUD = new AliasedUseDefInfov2(aa, _cgi, _tgi, bbm, _pairManager);
		}
		info.put(ICallGraphInfo.ID, _cgi);
		info.put(IThreadGraphInfo.ID, _tgi);
		info.put(PairManager.ID, _pairManager);
		info.put(IEnvironment.ID, aa.getEnvironment());
		info.put(IValueAnalyzer.ID, aa);
		info.put(IUseDefInfo.ALIASED_USE_DEF_ID, _aliasUD);
		info.put(IUseDefInfo.GLOBAL_USE_DEF_ID, _staticFieldUD);
		info.put(IStmtGraphFactory.ID, getStmtGraphFactory());

		final EquivalenceClassBasedEscapeAnalysis _ecba = new EquivalenceClassBasedEscapeAnalysis(_cgi, _tgi, getBbm());
		info.put(IEscapeInfo.ID, _ecba.getEscapeInfo());

		final IMonitorInfo _monitorInfo = new MonitorAnalysis();
		info.put(IMonitorInfo.ID, _monitorInfo);

		final SafeLockAnalysis _sla;

		if (useSafeLockAnalysis) {
			_sla = new SafeLockAnalysis();
			info.put(SafeLockAnalysis.ID, _sla);
		} else {
			_sla = null;
		}

		initialize();
		aa.analyze(getEnvironment(), getRootMethods());

		_callGraphInfoCollector.reset();
		_processors.clear();
		_processors.add(_callGraphInfoCollector);
		_pc.reset();
		_pc.driveProcessors(_processors);
		_cgi.createCallGraphInfo(_callGraphInfoCollector.getCallInfo());
		//writeInfo("CALL GRAPH:\n" + _cgi.toString());
//		if (commonUncheckedException) {
//			final ExceptionRaisingAnalysis _t = (ExceptionRaisingAnalysis) _eti;
//			_t.setupForCommonUncheckedExceptions();
//		}

		_processors.clear();
		((ThreadGraph) _tgi).reset();
		_processors.add((IProcessor) _tgi);
		//_processors.add((IProcessor) _eti);
		_processors.add(_countingProcessor);
		_cgipc.reset();
		_cgipc.driveProcessors(_processors);
		writeInfo("THREAD GRAPH:\n" + ((ThreadGraph) _tgi).toString());
		//writeInfo("EXCEPTION THROW INFO:\n" + ((ExceptionRaisingAnalysis) _eti).toString());
		//writeInfo("STATISTICS: " + MapUtils.verbosePrint(new TreeMap(_countingProcessor.getStatistics())));

		_aliasUD.hookup(_cgipc);
		_staticFieldUD.hookup(_cgipc);
		_cgipc.process();
		_staticFieldUD.unhook(_cgipc);
		_aliasUD.unhook(_cgipc);

		writeInfo("BEGIN: dependency analyses");

//		if (exceptionalExits) {
//			bbm = new BasicBlockGraphMgr(_eti);
//			bbm.setStmtGraphFactory(getStmtGraphFactory());
//		}

		final AnalysesController _ac = new AnalysesController(info, _cgipc, getBbm());
		_ac.addAnalyses(IMonitorInfo.ID, Collections.singleton((MonitorAnalysis) _monitorInfo));
		_ac.addAnalyses(EquivalenceClassBasedEscapeAnalysis.ID, Collections.singleton(_ecba));

		if (useSafeLockAnalysis) {
			_ac.addAnalyses(SafeLockAnalysis.ID, Collections.singleton(_sla));
		}

		for (final Iterator _i1 = das.iterator(); _i1.hasNext();) {
			try
			{
				final IDependencyAnalysis _da1 = (IDependencyAnalysis) _i1.next();
				//System.out.println("_da1.getIds().size()="+_da1.getIds().size());
				for (final Iterator<? extends Comparable<? extends Object>> _i2 = _da1.getIds().iterator(); _i2.hasNext();) {
					try
					{
						final Comparable<? extends Object> _id = _i2.next();
						_ac.addAnalyses(_id, Collections.singleton(_da1));
					}
					catch (Exception _e)
					{
						System.out.println("Exception: "+_e);
					}
				}

			}
			catch (Exception _e)
			{
				System.out.println("Exception: "+_e);
			}
		}

		_ac.initialize();
		_ac.execute();

		// write xml
		for (final Iterator _i1 = das.iterator(); _i1.hasNext();) {
			try
			{
				final IDependencyAnalysis _da1 = (IDependencyAnalysis) _i1.next();
	
				for (final Iterator _i2 = _da1.getIds().iterator(); _i2.hasNext();) {
					try
					{
						final Object _id = _i2.next();
						MapUtils.putIntoListInMap(info, _id, _da1);
					}
					catch (Exception _e)
					{
						System.out.println("Exception: "+_e);
					}
				}
			}
			catch (Exception _e)
			{
				System.out.println("Exception: "+_e);
			}
		}
		xmlizer.setGenerator(new UniqueJimpleIDGenerator());
		String keyStr="";
        for(Object key:info.keySet())
        {
        	System.out.println("  info Key: "+key);
        	keyStr=key.toString();
        	
        	if (keyStr.equals("READY_DA"))  {
//        		ReadyDAv1 _rd = new ReadyDAv1();
//        		writeInfo("READY_DA:\n" + ((ReadyDAv1) _rd).toString());
        		ArrayList infoValue= (ArrayList) info.get(key);
            	System.out.println("READY_DA info Value.size(): "+infoValue.size());
//            	for (int i=0; i<infoValue.size(); i++)
//            		System.out.println("READY_DA info Value["+i+"]="+infoValue.get(i));        		
        	}
        	else 
        	if (keyStr.equals("INTERFERENCE_DA"))  {
//        		IDependencyAnalysis _id = (IDependencyAnalysis) info.get(key);
//        		writeInfo("INTERFERENCE_DA:\n" + ((InterferenceDAv1) _id).toString());
        		ArrayList infoValue= (ArrayList) info.get(key);
            	System.out.println("INTERFERENCE_DA  info Value.size(): "+infoValue.size());
//            	for (int i=0; i<infoValue.size(); i++)
//            		System.out.println("INTERFERENCE_DA infoValue["+i+"]="+infoValue.get(i)); 
        	}
        	else 
        	if (keyStr.equals("SYNCHRONIZATION_DA"))  {
//        		IDependencyAnalysis _sd = (IDependencyAnalysis) info.get(key);
//        		writeInfo("SYNCHRONIZATION_DA:\n" + ((SynchronizationDA) _sd).toString());
        		ArrayList infoValue= (ArrayList) info.get(key);
            	System.out.println("SYNCHRONIZATION_DA  info Value.size(): "+infoValue.size());
//            	for (int i=0; i<infoValue.size(); i++)
//            		System.out.println("SYNCHRONIZATION_DA infoValue["+i+"]="+infoValue.get(i));
        	}
        }
		//xmlizer.writeXML(info);

		if (dumpJimple) {
			xmlizer.dumpJimple(null, xmlizer.getXmlOutputDir(), _xmlcgipc);
		}
		writeInfo("Total classes loaded: " + getEnvironment().getClasses().size());
		
		//SystemDependenceGraphBuilder.getSystemDependenceGraph(das, _cgi, getEnvironment().getClasses());
	}
}

// End of File
