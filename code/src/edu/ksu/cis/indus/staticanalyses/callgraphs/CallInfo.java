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

import edu.ksu.cis.indus.common.collections.SetUtils;
import edu.ksu.cis.indus.interfaces.ICallGraphInfo.CallTriple;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.builder.ToStringBuilder;

import soot.SootMethod;

/**
 * This is a data class that stores/provides data pertaining to call information. There is no processing element in this
 * class.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.7 $ $Date: 2007/02/10 19:07:01 $
 */
final class CallInfo
		implements CallGraphInfo.ICallInfo {

	/**
	 * This maps callees to callers.
	 */
	final Map<SootMethod, Collection<CallTriple>> callee2callers = new HashMap<SootMethod, Collection<CallTriple>>();

	/**
	 * This maps callers to callees.
	 */
	final Map<SootMethod, Collection<CallTriple>> caller2callees = new HashMap<SootMethod, Collection<CallTriple>>();

	/**
	 * The collection of methods that are reachble in the system.
	 */
	private final Set<SootMethod> reachables = new HashSet<SootMethod>();

	/**
	 * Optimization cache.
	 */
	private transient String toString;

	/**
	 * Records the given method as reachable.
	 * 
	 * @param method to be recorded.
	 * @pre method != null
	 */
	public void addReachable(final SootMethod method) {
		reachables.add(method);
	}

	/**
	 * @see CallGraphInfo.ICallInfo#getCallee2CallersMap()
	 */
	public Map<SootMethod, Collection<CallTriple>> getCallee2CallersMap() {
		return Collections.unmodifiableMap(callee2callers);
	}

	/**
	 * @see CallGraphInfo.ICallInfo#getCaller2CalleesMap()
	 */
	public Map<SootMethod, Collection<CallTriple>> getCaller2CalleesMap() {
		return Collections.unmodifiableMap(caller2callees);
	}

	/**
	 * @see CallGraphInfo.ICallInfo#getReachableMethods()
	 */
	public Collection<SootMethod> getReachableMethods() {
		return Collections.unmodifiableCollection(reachables);
	}

	/**
	 * Resets internal data structures.
	 */
	public void reset() {
		callee2callers.clear();
		caller2callees.clear();
		reachables.clear();
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override public String toString() {
		if (toString == null) {
			toString = new ToStringBuilder(this).appendSuper(super.toString()).append("reachables-size", reachables.size())
					.append("\n").append("caller2callees-size", caller2callees.size()).append("\n").append(
							"callee2callers-size", callee2callers.size()).append("reachables", reachables).append("\n")
					.append("caller2callees", caller2callees).append("\n").append("callee2callers", callee2callers)
					.toString();
		}
		return toString;
	}

	/**
	 * Injects empty sets for caller and callee information of methods with no callees and callers.
	 */
	void fixupMethodsHavingZeroCallersAndCallees() {
		final Iterator<SootMethod> _i = reachables.iterator();
		final int _iEnd = reachables.size();

		for (int _iIndex = 0; _iIndex < _iEnd; _iIndex++) {
			final SootMethod _o = _i.next();

			if (callee2callers.get(_o) == null) {
				callee2callers.put(_o, Collections.<CallTriple> emptySet());
			}

			if (caller2callees.get(_o) == null) {
				caller2callees.put(_o, Collections.<CallTriple> emptySet());
			}
		}

		//assert validate();
	}

	/**
	 * Validates the information. This is designed to be used inside an assertion.
	 * 
	 * @return <code>true</code> if the info is valid. An assertion violation is raised otherwise.
	 */
	private boolean validate() {
		final Collection<SootMethod> _k1 = caller2callees.keySet();
		for (final Iterator<Collection<CallTriple>> _i = callee2callers.values().iterator(); _i.hasNext();) {
			final Collection<CallTriple> _c = _i.next();
			for (final Iterator<CallTriple> _j = _c.iterator(); _j.hasNext();) {
				final CallTriple _ctrp = _j.next();
				assert _k1.contains(_ctrp.getMethod());
			}
		}

		final Collection<SootMethod> _k2 = callee2callers.keySet();
		for (final Iterator<Collection<CallTriple>> _i = caller2callees.values().iterator(); _i.hasNext();) {
			final Collection<CallTriple> _c = _i.next();
			for (final Iterator<CallTriple> _j = _c.iterator(); _j.hasNext();) {
				final CallTriple _ctrp = _j.next();
				assert _k2.contains(_ctrp.getMethod());
			}
		}

		assert _k1.containsAll(reachables) : SetUtils.difference(reachables, _k1);
		assert _k2.containsAll(reachables) : SetUtils.difference(reachables, _k2);
		assert reachables.containsAll(_k1) : SetUtils.difference(_k1, reachables);
		assert reachables.containsAll(_k2) : SetUtils.difference(_k2, reachables);

		return true;
	}

}

// End of File
