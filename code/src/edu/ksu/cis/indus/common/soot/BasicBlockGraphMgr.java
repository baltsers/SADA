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

package edu.ksu.cis.indus.common.soot;

import edu.ksu.cis.indus.annotations.Empty;
import edu.ksu.cis.indus.annotations.Immutable;
import edu.ksu.cis.indus.annotations.NonNull;
import edu.ksu.cis.indus.annotations.NonNullContainer;
import edu.ksu.cis.indus.common.collections.IteratorUtils;
import edu.ksu.cis.indus.interfaces.IExceptionRaisingInfo;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.toolkits.graph.UnitGraph;

/**
 * This class manages a set of basic block graphs.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.21 $ $Date: 2007/02/10 19:08:36 $
 */
public final class BasicBlockGraphMgr {

	/**
	 * The logger used by instances of this class to log messages.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(BasicBlockGraphMgr.class);

	/**
	 * This provides exception throwing information used to calculate basic block boundaries.
	 */
	private final IExceptionRaisingInfo eti;

	/**
	 * This maps methods to basic block graphs.
	 */
	@NonNull @NonNullContainer private final Map<SootMethod, Reference<BasicBlockGraph>> method2graph;

	/**
	 * This maps methods to their statement list.
	 */
	@NonNull @NonNullContainer private final Map<SootMethod, List<Stmt>> method2stmtlist = new HashMap<SootMethod, List<Stmt>>();
	@NonNull @NonNullContainer private final Map<SootMethod, List<Unit>> method2unitlist = new HashMap<SootMethod, List<Unit>>();
	/**
	 * This provides <code>UnitGraph</code>s required to construct the basic block graphs.
	 */
	private IStmtGraphFactory<?> stmtGraphProvider;

	/**
	 * Creates a new BasicBlockGraphMgr object.
	 */
	@Empty public BasicBlockGraphMgr() {
		this(null);
	}

	/**
	 * Creates an instance of this class.
	 * 
	 * @param info provides excpetion throwing information. If this is not provided then implicit exceptional exits are not
	 *            considered for graph construction.
	 */
	public BasicBlockGraphMgr(@Immutable final IExceptionRaisingInfo info) {
		super();
		eti = info;
		method2graph = new HashMap<SootMethod, Reference<BasicBlockGraph>>(Constants.getNumOfMethodsInApplication());
	}

	/**
	 * Retrieves the basic block graph corresponding to the given method. Returns an empty basic block graph if the method is
	 * abstract or has no available implementation.
	 * 
	 * @param sm is the method for which the graph is requested.
	 * @return the basic block graph corresponding to <code>sm</code>.
	 * @throws IllegalStateException when a statement graph factory was not set before calling this method.
	 */
	public BasicBlockGraph getBasicBlockGraph(@NonNull final SootMethod sm) throws IllegalStateException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getBasicBlockGraph(SootMethod sm = " + sm + ") - BEGIN");
		}

		if (stmtGraphProvider == null) {
			throw new IllegalStateException("You need to set the unit graph provider via setStmtGraphFactory() before "
					+ "calling this method.");
		}

		final Reference<BasicBlockGraph> _ref = method2graph.get(sm);
		BasicBlockGraph _result = null;
		boolean _flag = false;

		if (_ref == null) {
			_flag = true;
		} else {
			_result = _ref.get();

			if (_result == null) {
				_flag = true;
			}
		}

		if (_flag) {
			final UnitGraph _graph = stmtGraphProvider.getStmtGraph(sm);
			_result = new BasicBlockGraph(_graph, sm, eti);
			method2graph.put(sm, new SoftReference<BasicBlockGraph>(_result));
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getBasicBlockGraph() - END - return value = " + _result);
		}
		return _result;
	}

	/**
	 * Provides the unit graph for the given method. This is retrieved from the unit graph provider set via
	 * <code>setUnitGraphProvider</code>.
	 * 
	 * @param method for which the unit graph is requested.
	 * @return the unit graph for the method.
	 */
	@NonNull public UnitGraph getStmtGraph(@NonNull final SootMethod method) {
		return stmtGraphProvider.getStmtGraph(method);
	}

	/**
	 * Returns an unmodifiable list of statements of the given method represented in this graph.
	 * 
	 * @param method of interest.
	 * @return an unmodifiable list of statements.
	 */
//	@NonNull @NonNullContainer public List<Stmt> getStmtList(@NonNull final SootMethod method) {
//		if (LOGGER.isDebugEnabled()) {
//			LOGGER.debug("getStmtList(method = " + method + ")");
//		}
//
//		List<Stmt> _result = method2stmtlist.get(method);
//
//		if (_result == null) {
//			final UnitGraph _stmtGraph = getStmtGraph(method);
//
//			if (_stmtGraph != null) {
//				@SuppressWarnings("unchecked") final List<Unit> _toList = IteratorUtils.toList(_stmtGraph.iterator());
//				_result = Collections.unmodifiableList(_toList);
//			} else {
//				_result = Collections.emptyList();
//			}
//			method2stmtlist.put(method, _result);
//		}
//		return _result;
//	}
	@NonNull @NonNullContainer public List<Stmt> getStmtList(@NonNull final SootMethod method) {
		List<Unit> _resultUnit = getUnitList(method);
		
		List<Stmt> _result = method2stmtlist.get(method);
		if (_result == null)  {
			Iterator<Unit> iter = _resultUnit.iterator();
			while(iter.hasNext()){  
				_result.add( (Stmt) iter.next());
			}
		}
		return _result;
	}

	@NonNull @NonNullContainer public List<Unit> getUnitList(@NonNull final SootMethod method) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getUnitList(method = " + method + ")");
		}

		List<Unit> _result = method2unitlist.get(method);

		if (_result == null) {
			final UnitGraph _UnitGraph = getStmtGraph(method);

			if (_UnitGraph != null) {
				@SuppressWarnings("unchecked") final List<Unit> _toList = IteratorUtils.toList(_UnitGraph.iterator());
				_result = Collections.unmodifiableList(_toList);
			} else {
				_result = Collections.emptyList();
			}
			method2unitlist.put(method, _result);
		}
		return _result;
	}
	/**
	 * Resets the internal data structures.
	 */
	public void reset() {
		method2graph.clear();
		method2stmtlist.clear();
	}

	/**
	 * Sets the unit graph provider.
	 * 
	 * @param <T> is the type of cfgs provided by the factory.
	 * @param cfgProvider provides <code>UnitGraph</code>s required to construct the basic block graphs.
	 */
	public <T extends UnitGraph> void setStmtGraphFactory(@NonNull @Immutable final IStmtGraphFactory<T> cfgProvider) {
		stmtGraphProvider = cfgProvider;
	}
}

// End of File
