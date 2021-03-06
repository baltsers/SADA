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

package edu.ksu.cis.indus.staticanalyses.callgraphs;

import edu.ksu.cis.indus.common.ToStringBasedComparator;
import edu.ksu.cis.indus.common.collections.Cache;
import edu.ksu.cis.indus.common.collections.CollectionUtils;
import edu.ksu.cis.indus.common.collections.IPredicate;
import edu.ksu.cis.indus.common.collections.MapUtils;
import edu.ksu.cis.indus.common.datastructures.Pair;
import edu.ksu.cis.indus.common.datastructures.Triple;
import edu.ksu.cis.indus.common.graph.GraphReachabilityPredicate;
import edu.ksu.cis.indus.common.graph.IDirectedGraph;
import edu.ksu.cis.indus.common.graph.SimpleNode;
import edu.ksu.cis.indus.common.graph.SimpleNodeGraph;
import edu.ksu.cis.indus.common.soot.Constants;
import edu.ksu.cis.indus.interfaces.AbstractStatus;
import edu.ksu.cis.indus.interfaces.ICallGraphInfo;
import edu.ksu.cis.indus.processing.Context;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.SootMethod;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

/**
 * This class calculates call graphCache information from the given object flow analysis.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath </a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.26 $
 */
public final class CallGraphInfo
		extends AbstractStatus
		implements ICallGraphInfo {

	/**
	 * This is the interface is used to access call info in it's primitive form.
	 * 
	 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath </a>
	 * @author $Author: rvprasad $
	 * @version $Revision: 1.26 $
	 */
	public static interface ICallInfo {

		/**
		 * Retrieves the callee to callers map.
		 * 
		 * @return a map for callee to callers.
		 * @post result != null
		 */
		Map<SootMethod, Collection<CallTriple>> getCallee2CallersMap();

		/**
		 * Retrieves the caller to callees map.
		 * 
		 * @return a map for caller to callees.
		 * @post result != null
		 */
		Map<SootMethod, Collection<CallTriple>> getCaller2CalleesMap();

		/**
		 * Retrieves the methods reachable in the system.
		 * 
		 * @return a collection of reachable methods.
		 * @post result != null
		 */
		Collection<SootMethod> getReachableMethods();
	}

	/**
	 * A comparator to compare call triples based on <code>toString()</code> value of the method being called.
	 * 
	 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath </a>
	 * @author $Author: rvprasad $
	 * @version $Revision: 1.26 $ $Date: 2007/02/10 19:07:01 $
	 */
	private static class CallTripleMethodToStringBasedComparator
			implements Comparator<CallTriple> {

		/**
		 * @see Comparator#compare(Object,Object)
		 */
		public int compare(final CallTriple o1, final CallTriple o2) {
			final int _i = o1.getMethod().getSignature().compareTo(o2.getMethod().getSignature());
			return _i == 0 ? o1.toString().compareTo(o2.toString()) : _i;
		}
	}

	/**
	 * The constant that controls the connectivity cache size. This constant ensures that the cache is large enough to store
	 * all mappings under the assumption that every method is called at most 3 times.
	 */
	private static final int CONNECTIVITY_CACHE_SIZE = Constants.getNumOfMethodsInApplication() * 3;

	/**
	 * The logger used by instances of this class to log messages.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(CallGraphInfo.class);

	/**
	 * This maps callees to callers.
	 */
	private final Map<SootMethod, Collection<CallTriple>> callee2callers = new HashMap<SootMethod, Collection<CallTriple>>();

	/**
	 * This cache stores callee to call-site reachability information. The cache is large enough to store all mappings under
	 * the assumption that every method is called at most 3 times.
	 */
	private Map<Triple<SootMethod, Stmt, SootMethod>, Boolean> calleeCallSiteReachabilityCache = new Cache<Triple<SootMethod, Stmt, SootMethod>, Boolean>(
			CONNECTIVITY_CACHE_SIZE);

	/**
	 * This maps callers to callees.
	 */
	private final Map<SootMethod, Collection<CallTriple>> caller2callees = new HashMap<SootMethod, Collection<CallTriple>>();

	/**
	 * This caches a traversable graphCache representation of the call graphCache.
	 */
	private SimpleNodeGraph<SootMethod> graphCache;

	/**
	 * The collection of methods that don't have callers in the system. These typically include root methods and class
	 * initializers.
	 */
	private final Collection<SootMethod> heads = new HashSet<SootMethod>();

	/**
	 * A cache of mappings from an invocation site to methods reachable from that site via call chain.
	 */
	private final Map<Pair<Stmt, SootMethod>, Collection<SootMethod>> invocationsite2reachableMethods = new HashMap<Pair<Stmt, SootMethod>, Collection<SootMethod>>(
			Constants.getNumOfMethodsInApplication());

	/**
	 * A cache of mappings from a method to methods that can via a call chain reach the key of the mapping.
	 */
	private final Map<SootMethod, Collection<SootMethod>> method2backwardReachableMethods = new HashMap<SootMethod, Collection<SootMethod>>(
			Constants.getNumOfMethodsInApplication());

	/**
	 * A cache of mappings from a method to methods reachable from that site via call chain.
	 */
	private final Map<SootMethod, Collection<SootMethod>> method2forwardReachableMethods = new HashMap<SootMethod, Collection<SootMethod>>(
			Constants.getNumOfMethodsInApplication());

	/**
	 * This manages pair objects.
	 */
	private final Pair.PairManager pairMgr;

	/**
	 * The collection of methods that are reachble in the system.
	 */
	private final Collection<SootMethod> reachables = new HashSet<SootMethod>();

	/**
	 * The collection of SCCs in this call graph in top-down direction.
	 */
	private WeakReference<List<List<SootMethod>>> topDownSCC;

	/**
	 * Creates a new CallGraphInfo object.
	 * 
	 * @param pairManager to be used when creating pair objects.
	 * @pre pairManager != null
	 */
	public CallGraphInfo(final Pair.PairManager pairManager) {
		pairMgr = pairManager;
	}

	/**
	 * @see edu.ksu.cis.indus.interfaces.ICallGraphInfo#areAnyMethodsReachableFrom(java.util.Collection, soot.SootMethod)
	 */
	public boolean areAnyMethodsReachableFrom(final Collection<SootMethod> methods, final SootMethod caller) {
		final IPredicate<SootMethod> _pred = new IPredicate<SootMethod>() {

			public boolean evaluate(final SootMethod object) {
				return isCalleeReachableFromCaller(object, caller);
			}
		};
		return CollectionUtils.exists(methods, _pred);
	}

	/**
	 * @see ICallGraphInfo#areAnyMethodsReachableFrom(java.util.Collection, soot.jimple.Stmt, soot.SootMethod)
	 */
	public boolean areAnyMethodsReachableFrom(final Collection<SootMethod> methods, final Stmt stmt, final SootMethod caller) {
		final IPredicate<SootMethod> _pred = new IPredicate<SootMethod>() {

			public boolean evaluate(final SootMethod object) {
				return isCalleeReachableFromCallSite(object, stmt, caller);
			}
		};
		return CollectionUtils.exists(methods, _pred);
	}

	/**
	 * Constructs call graph from the information provided by the given provider.
	 * 
	 * @param provider provides call information to be consolidated.
	 */
	public void createCallGraphInfo(final ICallInfo provider) {
		callee2callers.putAll(provider.getCallee2CallersMap());
		caller2callees.putAll(provider.getCaller2CalleesMap());
		reachables.addAll(provider.getReachableMethods());
		calculateHeads();
		createGraph();
		stable();

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("createCallGraphInfo(ICallInfo) - call-graph: \n" + toString());
		}
	}

	/**
	 * Returns the set of method implementations that shall be invoked at the given callsite expression in the given method.
	 * 
	 * @param invokeExpr the method call site.
	 * @param context in which the call occurs.
	 * @return a collection of methods.
	 * @pre invokeExpr != null and context != null
	 * @pre context.getCurrentMethod() != null
	 * @pre contet.getStmt() != null
	 * @post result != null
	 * @see edu.ksu.cis.indus.interfaces.ICallGraphInfo#getCallees(InvokeExpr,Context)
	 */
	public Collection<SootMethod> getCallees(final InvokeExpr invokeExpr, final Context context) {
		final Collection<SootMethod> _result;

		final SootMethod _currentMethod = context.getCurrentMethod();
		if (caller2callees.containsKey(_currentMethod)) {
			_result = new ArrayList<SootMethod>();

			for (final Iterator<CallTriple> _i = caller2callees.get(_currentMethod).iterator(); _i.hasNext();) {
				final CallTriple _ctrp = _i.next();

				if (_ctrp.getExpr().equals(invokeExpr)) {
					_result.add(_ctrp.getMethod());
				}
			}
		} else {
			_result = Collections.emptyList();
		}

		return _result;
	}

	/**
	 * Returns a collection of methods called by <code>caller</code>.
	 * 
	 * @param caller which calls the returned methods.
	 * @return a collection of call sites along with callees at those sites.
	 * @pre caller != null
	 * @post result != null and result.oclIsKindOf(Collection(CallTriple))
	 * @see edu.ksu.cis.indus.interfaces.ICallGraphInfo#getCallees(SootMethod)
	 */
	public Collection<CallTriple> getCallees(final SootMethod caller) {
		final Collection<CallTriple> _callees = MapUtils.queryCollection(caller2callees, caller);
		return Collections.unmodifiableCollection(_callees);
	}

	/**
	 * Returns the methods that call the given method independent of any context.
	 * 
	 * @param callee is the method being called.
	 * @return a collection of call-sites at which <code>callee</code> is called.
	 * @pre callee != null
	 * @post result != null
	 * @see edu.ksu.cis.indus.interfaces.ICallGraphInfo#getCallers(soot.SootMethod)
	 */
	public Collection<CallTriple> getCallers(final SootMethod callee) {
		final Collection<CallTriple> _callers = MapUtils.queryCollection(callee2callers, callee);
		return Collections.unmodifiableCollection(_callers);
	}

	/**
	 * @see ICallGraphInfo#getCommonMethodsReachableFrom(soot.SootMethod, boolean, soot.SootMethod, boolean)
	 */
	public Collection<SootMethod> getCommonMethodsReachableFrom(final SootMethod method1, final boolean forward1,
			final SootMethod method2, final boolean forward2) {
		final Collection<SootMethod> _result;

		if (graphCache.hasCommonReachablesFrom(graphCache.queryNode(method1), forward1, graphCache.queryNode(method2),
				forward2)) {
			final Collection<SimpleNode<SootMethod>> _r = graphCache.getCommonReachablesFrom(graphCache.queryNode(method1),
					forward1, graphCache.queryNode(method2), forward2);
			_result = CollectionUtils.collect(_r, graphCache.getObjectExtractor());
		} else {
			_result = Collections.emptyList();
		}
		return _result;
	}

	/**
	 * @see ICallGraphInfo#getConnectivityCalleesFor(soot.SootMethod, soot.SootMethod)
	 */
	public Collection<SootMethod> getConnectivityCalleesFor(final SootMethod method1, final SootMethod method2) {
		final Collection<SootMethod> _result;

		if (graphCache.hasCommonReachablesFrom(graphCache.queryNode(method1), true, graphCache.queryNode(method2), true)) {
			final Collection<SimpleNode<SootMethod>> _r = graphCache.getConnectivityNodesFor(graphCache.queryNode(method1),
					graphCache.queryNode(method2), true);
			_result = CollectionUtils.collect(_r, graphCache.getObjectExtractor());
		} else {
			_result = Collections.emptyList();
		}
		return _result;
	}

	/**
	 * @see ICallGraphInfo#getConnectivityCallersFor(soot.SootMethod, soot.SootMethod)
	 */
	public Collection<SootMethod> getConnectivityCallersFor(final SootMethod method1, final SootMethod method2) {
		final Collection<SootMethod> _result;

		if (graphCache.hasCommonReachablesFrom(graphCache.queryNode(method1), false, graphCache.queryNode(method2), false)) {
			final Collection<SimpleNode<SootMethod>> _r = graphCache.getConnectivityNodesFor(graphCache.queryNode(method1),
					graphCache.queryNode(method2), false);
			_result = CollectionUtils.collect(_r, graphCache.getObjectExtractor());
		} else {
			_result = Collections.emptyList();
		}
		return _result;
	}

	/**
	 * Returns the methods that are the entry point for the analyzed system.
	 * 
	 * @return a collection of methods.
	 * @post result != null
	 * @see edu.ksu.cis.indus.interfaces.ICallGraphInfo#getEntryMethods()
	 */
	public Collection<SootMethod> getEntryMethods() {
		return Collections.unmodifiableCollection(heads);
	}

	/**
	 * @see edu.ksu.cis.indus.interfaces.IIdentification#getIds()
	 */
	public Collection<? extends Comparable<?>> getIds() {
		return Collections.singleton(ICallGraphInfo.ID);
	}

	/**
	 * @see edu.ksu.cis.indus.interfaces.ICallGraphInfo#getMethodsInTopologicalOrder(boolean)
	 */
	public List<SootMethod> getMethodsInTopologicalOrder(final boolean topdown) {
		final List<SimpleNode<SootMethod>> _topologicalSorted = graphCache.performTopologicalSort(topdown);
		final List<SootMethod> _result = new ArrayList<SootMethod>();
		CollectionUtils.transform(_topologicalSorted, graphCache.getObjectExtractor(), _result);
		return _result;
	}

	/**
	 * @see edu.ksu.cis.indus.interfaces.ICallGraphInfo#getMethodsReachableFrom(soot.SootMethod,boolean)
	 */
	public Collection<SootMethod> getMethodsReachableFrom(final SootMethod root, final boolean forward) {
		return Collections.unmodifiableCollection(getMethodsReachableFromHelper(root, forward));
	}

	/**
	 * @see edu.ksu.cis.indus.interfaces.ICallGraphInfo#getMethodsReachableFrom(soot.jimple.Stmt,soot.SootMethod)
	 */
	public Collection<SootMethod> getMethodsReachableFrom(final Stmt stmt, final SootMethod root) {
		final Pair<Stmt, SootMethod> _pair = pairMgr.getPair(stmt, root);
		Collection<SootMethod> _result = invocationsite2reachableMethods.get(_pair);

		if (_result == null) {
			final InvokeExpr _ie = stmt.getInvokeExpr();
			final Context _context = new Context();
			_context.setRootMethod(root);

			final Collection<SootMethod> _callees = getCallees(_ie, _context);
			final Collection<SootMethod> _methods = new ArrayList<SootMethod>();
			_methods.addAll(_callees);

			for (final Iterator<SootMethod> _i = _callees.iterator(); _i.hasNext();) {
				_methods.addAll(getMethodsReachableFromHelper(_i.next(), true));
			}
			invocationsite2reachableMethods.put(_pair, _methods);
			_result = _methods;
		}
		return Collections.unmodifiableCollection(_result);
	}

	/**
	 * Returns the methods reachable in the analyzed system.
	 * 
	 * @return a collection of methods.
	 * @post result != null
	 * @see edu.ksu.cis.indus.interfaces.ICallGraphInfo#getReachableMethods()
	 */
	public Collection<SootMethod> getReachableMethods() {
		return Collections.unmodifiableCollection(reachables);
	}

	/**
	 * @see edu.ksu.cis.indus.interfaces.ICallGraphInfo#getSCCs(boolean)
	 */
	public List<List<SootMethod>> getSCCs(final boolean topDown) {
		List<List<SootMethod>> _r = topDownSCC == null ? null : topDownSCC.get();
		if (_r == null) {
			_r = new ArrayList<List<SootMethod>>();
			final List<List<SimpleNode<SootMethod>>> _temp = graphCache.getSCCs(true);

			for (final Iterator<List<SimpleNode<SootMethod>>> _i = _temp.iterator(); _i.hasNext();) {
				final List<SimpleNode<SootMethod>> _scc = _i.next();
				_r.add(Collections.unmodifiableList(CollectionUtils.collect(_scc, graphCache.getObjectExtractor())));
			}
			topDownSCC = new WeakReference<List<List<SootMethod>>>(_r);
		}
		final List<List<SootMethod>> _result = new ArrayList<List<SootMethod>>(_r);

		if (!topDown) {
			Collections.reverse(_result);
		}
		return _result;
	}

	/**
	 * @see edu.ksu.cis.indus.interfaces.ICallGraphInfo#isCalleeReachableFromCaller(soot.SootMethod, soot.SootMethod)
	 */
	public boolean isCalleeReachableFromCaller(final SootMethod callee, final SootMethod caller) {
		final SimpleNode<SootMethod> _calleeNode = graphCache.queryNode(callee);
		final SimpleNode<SootMethod> _callerNode = graphCache.queryNode(caller);
		return _calleeNode != null && _callerNode != null && graphCache.isReachable(_callerNode, _calleeNode, true);
	}

	/**
	 * @see ICallGraphInfo#isCalleeReachableFromCallSite(soot.SootMethod, Stmt, soot.SootMethod)
	 */
	public boolean isCalleeReachableFromCallSite(final SootMethod callee, final Stmt stmt, final SootMethod caller) {
		final boolean _result;
		final Triple<SootMethod, Stmt, SootMethod> _trp = new Triple<SootMethod, Stmt, SootMethod>(callee, stmt, caller);

		if (!calleeCallSiteReachabilityCache.containsKey(_trp)) {
			final SimpleNode<SootMethod> _n = graphCache.queryNode(callee);
			final IPredicate<SootMethod> _rp = new GraphReachabilityPredicate<SimpleNode<SootMethod>, SootMethod>(_n, true,
					graphCache);

			if (_rp.evaluate(caller)) {
				final InvokeExpr _ie = stmt.getInvokeExpr();
				final Context _context = new Context();
				_context.setRootMethod(caller);

				final Collection<SootMethod> _methodsThatMayCallCallee = getCallees(_ie, _context);
				_result = _methodsThatMayCallCallee.contains(callee)
						|| CollectionUtils.exists(_methodsThatMayCallCallee, _rp);
			} else {
				_result = false;
			}
			calleeCallSiteReachabilityCache.put(_trp, Boolean.valueOf(_result));
		} else {
			_result = calleeCallSiteReachabilityCache.get(_trp).booleanValue();
		}

		return _result;
	}

	/**
	 * Checks if the given method is reachable in the analyzed system.
	 * 
	 * @param method to be checked for reachabiliy.
	 * @return <code>true</code> if <code>method</code> is reachable; <code>false</code>, otherwise.
	 * @pre method != null
	 * @see edu.ksu.cis.indus.interfaces.ICallGraphInfo#isReachable(soot.SootMethod)
	 */
	public boolean isReachable(final SootMethod method) {
		return reachables.contains(method);
	}

	/**
	 * Resets all internal data structure and forgets all info from the previous run.
	 */
	public void reset() {
		unstable();
		caller2callees.clear();
		callee2callers.clear();
		graphCache = null;
		topDownSCC = null;
		reachables.clear();
		heads.clear();
		method2forwardReachableMethods.clear();
		invocationsite2reachableMethods.clear();
		calleeCallSiteReachabilityCache.clear();
	}

	/**
	 * Provides a stringized representation of this call graphCache.
	 * 
	 * @return stringized representation of the this call graphCache.
	 */
	@Override public String toString() {
		final StringBuffer _result = new StringBuffer();
		final List<SootMethod> _temp1 = new ArrayList<SootMethod>();
		String midMethodSign="";
		_result.append("Roots of the system: ");
		_temp1.addAll(getEntryMethods());
		Collections.sort(_temp1, ToStringBasedComparator.getComparator());
		//System.out.println( "_temp1.size()="+_temp1.size());
		for (final Iterator<SootMethod> _i = _temp1.iterator(); _i.hasNext();) {
			midMethodSign=_i.next().getSignature();
			if (!midMethodSign.startsWith("<java.") && !midMethodSign.startsWith("<sun."))
				_result.append("\t" + midMethodSign);
		}

		//_result.append("\nReachable methods in the system: " + getReachableMethods().size() + "\n" + getReachableMethods());
		_result.append("\nStrongly Connected components in the system: " + getSCCs(true).size() + "\n");
		_result.append("\n******************* top-down\n");

		_temp1.clear();
		_temp1.addAll(caller2callees.keySet());
		Collections.sort(_temp1, ToStringBasedComparator.getComparator());

		final List<CallTriple> _temp3 = new ArrayList<CallTriple>();
		for (final Iterator<SootMethod> _i = _temp1.iterator(); _i.hasNext();) {
			final SootMethod _caller = _i.next();
			midMethodSign=_caller.getSignature();
			if (midMethodSign.startsWith("<java.") || midMethodSign.startsWith("<sun."))
				continue;
			_result.append("\n" + _caller.getSignature() + "\n");
			_temp3.clear();
			_temp3.addAll(caller2callees.get(_caller));
			Collections.sort(_temp3, new CallTripleMethodToStringBasedComparator());

			for (final Iterator<CallTriple> _j = _temp3.iterator(); _j.hasNext();) {
				final CallTriple _ctrp = _j.next();
				_result.append("\t" + _ctrp + "\n");
			}
		}

		_result.append("\n************************* bottom-up\n");
		_temp1.clear();
		_temp1.addAll(callee2callers.keySet());
		Collections.sort(_temp1, ToStringBasedComparator.getComparator());

		for (final Iterator<SootMethod> _i = _temp1.iterator(); _i.hasNext();) {
			final SootMethod _callee = _i.next();
			midMethodSign=_callee.getSignature();
			if (midMethodSign.startsWith("<java.")|| midMethodSign.startsWith("<sun."))
				continue;
			_result.append("\n" + _callee.getSignature() + "\n");
			_temp3.clear();
			_temp3.addAll(callee2callers.get(_callee));
			Collections.sort(_temp3, new CallTripleMethodToStringBasedComparator());

			for (final Iterator<CallTriple> _j = _temp3.iterator(); _j.hasNext();) {
				final CallTriple _ctrp = _j.next();
				_result.append("\t" + _ctrp.getMethod().getSignature() + "\n");
			}
		}

		return _result.toString();
	}
	public String toFullString() {
		final StringBuffer _result = new StringBuffer();
		final List<SootMethod> _temp1 = new ArrayList<SootMethod>();

		_result.append("Roots of the system: ");
		_temp1.addAll(getEntryMethods());
		Collections.sort(_temp1, ToStringBasedComparator.getComparator());

		for (final Iterator<SootMethod> _i = _temp1.iterator(); _i.hasNext();) {
			_result.append("\t" + _i.next().getSignature());
		}

		_result.append("\nReachable methods in the system: " + getReachableMethods().size() + "\n" + getReachableMethods());
		_result.append("Strongly Connected components in the system: " + getSCCs(true).size() + "\n");
		_result.append("top-down\n");

		_temp1.clear();
		_temp1.addAll(caller2callees.keySet());
		Collections.sort(_temp1, ToStringBasedComparator.getComparator());

		final List<CallTriple> _temp3 = new ArrayList<CallTriple>();
		for (final Iterator<SootMethod> _i = _temp1.iterator(); _i.hasNext();) {
			final SootMethod _caller = _i.next();
			_result.append("\n" + _caller.getSignature() + "\n");
			_temp3.clear();
			_temp3.addAll(caller2callees.get(_caller));
			Collections.sort(_temp3, new CallTripleMethodToStringBasedComparator());

			for (final Iterator<CallTriple> _j = _temp3.iterator(); _j.hasNext();) {
				final CallTriple _ctrp = _j.next();
				_result.append("\t" + _ctrp + "\n");
			}
		}

		_result.append("bottom-up\n");
		_temp1.clear();
		_temp1.addAll(callee2callers.keySet());
		Collections.sort(_temp1, ToStringBasedComparator.getComparator());

		for (final Iterator<SootMethod> _i = _temp1.iterator(); _i.hasNext();) {
			final SootMethod _callee = _i.next();
			_result.append("\n" + _callee.getSignature() + "\n");
			_temp3.clear();
			_temp3.addAll(callee2callers.get(_callee));
			Collections.sort(_temp3, new CallTripleMethodToStringBasedComparator());

			for (final Iterator<CallTriple> _j = _temp3.iterator(); _j.hasNext();) {
				final CallTriple _ctrp = _j.next();
				_result.append("\t" + _ctrp.getMethod().getSignature() + "\n");
			}
		}

		return _result.toString();
	}

	/**
	 * Testing purposes only.
	 * 
	 * @return the cached copy of the call graph.
	 */
	IDirectedGraph<?> getCallGraph() {
		return graphCache;
	}

	/**
	 * Calculates head methods.
	 */
	private void calculateHeads() {
		final Set<SootMethod> _keySet = callee2callers.keySet();
		final Iterator<SootMethod> _i = _keySet.iterator();
		final int _iEnd = _keySet.size();
		heads.addAll(reachables);

		for (int _iIndex = 0; _iIndex < _iEnd; _iIndex++) {
			final SootMethod _method = _i.next();

			if (!callee2callers.get(_method).isEmpty()) {
				heads.remove(_method);
			}
		}
	}

	/**
	 * Creates a call graph.
	 */
	private void createGraph() {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Starting construction of call graph...");
		}

		// construct call graph
		graphCache = new SimpleNodeGraph<SootMethod>();
		String midMethodSign="";
		for (final Iterator<SootMethod> _i = reachables.iterator(); _i.hasNext();) {
			final SootMethod _caller = _i.next();
			midMethodSign=_caller.getSignature();
			if (midMethodSign.startsWith("<java.") || midMethodSign.startsWith("<sun."))
				continue;			
			final SimpleNode<SootMethod> _callerNode = graphCache.getNode(_caller);
			final Collection<CallTriple> _callees = MapUtils.queryCollection(caller2callees, _caller);

			if (!_callees.isEmpty()) {
				for (final Iterator<CallTriple> _j = _callees.iterator(); _j.hasNext();) {
					final CallTriple _ctrp = _j.next();
					final SootMethod _method = _ctrp.getMethod();

					graphCache.addEdgeFromTo(_callerNode, graphCache.getNode(_method));
				}
			}
		}

		graphCache.setConnectivityCacheSize(CONNECTIVITY_CACHE_SIZE);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Call Graph : " + toString());
		}

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("END: call graph consolidation");
		}
	}

	/**
	 * Retrieves the reachables in the given direction. The returned value exposes private data. Hence, callers should address
	 * the issue of keeping this data private.
	 * 
	 * @param root see IDirectedGraph.getReachableFrom(INode, boolean)
	 * @param forward see IDirectedGraph.getReachableFrom(INode, boolean)
	 * @return see IDirectedGraph.getReachableFrom(INode, boolean)
	 * @pre root != null
	 * @see IDirectedGraph#getReachablesFrom(edu.ksu.cis.indus.common.graph.INode, boolean)
	 */
	private Collection<SootMethod> getMethodsReachableFromHelper(final SootMethod root, final boolean forward) {
		final Map<SootMethod, Collection<SootMethod>> _map;

		if (forward) {
			_map = method2forwardReachableMethods;
		} else {
			_map = method2backwardReachableMethods;
		}

		Collection<SootMethod> _result = _map.get(root);

		if (_result == null) {
			_result = new ArrayList<SootMethod>();
			CollectionUtils.transform(graphCache.getReachablesFrom(graphCache.queryNode(root), forward), graphCache
					.getObjectExtractor(), _result);
			_map.put(root, _result);
		}
		return _result;
	}
}

// End of File
