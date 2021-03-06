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

import edu.ksu.cis.indus.annotations.Empty;
import edu.ksu.cis.indus.common.collections.MapUtils;
import edu.ksu.cis.indus.common.datastructures.HistoryAwareFIFOWorkBag;
import edu.ksu.cis.indus.common.datastructures.IWorkBag;
import edu.ksu.cis.indus.common.datastructures.Pair;
import edu.ksu.cis.indus.common.soot.BasicBlockGraphMgr;
import edu.ksu.cis.indus.common.soot.Constants;
import edu.ksu.cis.indus.processing.IProcessor;
import edu.ksu.cis.indus.staticanalyses.InitializationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import soot.SootMethod;

/**
 * This class provides dependence information closure. In other words, given a dependence analysis, it provides the indirect
 * version of it.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.18 $ $Date: 2007/02/10 19:07:02 $
 * @param <E1> is the type of dependee object in the context of dependee-to-dependent info maintenance.
 * @param <C1> is the type of context object in the context of dependee-to-dependent info maintenance.
 * @param <T1> is the type of dependent object in the context of dependee-to-dependent info maintenance.
 * @param <KE> is the type of key object in the context of dependee-to-dependent info maintenance.
 * @param <VT> is the type of value object in the context of dependee-to-dependent info maintenance.
 * @param <T2> is the type of dependent object in the context of dependent-to-dependee info maintenance.
 * @param <C2> is the type of context object in the context of dependent-to-dependee info maintenance.
 * @param <E2> is the type of dependee object in the context of dependent-to-dependee info maintenance.
 * @param <KT> is the type of key object in the context of dependent-to-dependee info maintenance.
 * @param <VE> is the type of value object in the context of dependent-to-dependee info maintenance.
 */
final class IndirectDependenceAnalysis<T1, C1, E1, KE, VT, E2, C2, T2, KT, VE>
		implements IDependencyAnalysis<T1, C1, E1, E2, C2, T2> {

	/**
	 * This analysis provides seed dependence information.
	 */
	private final IDependencyAnalysis<T1, C1, E1, E2, C2, T2> da;

	/**
	 * This is similar to <code>dependent2dependee</code> except the direction is dependee->dependent. Hence, it is
	 * recommended that the subclass use this store dependence information.
	 *
	 * @invariant dependee2dependent != null
	 */
	private final Map<Pair<E2, C2>, Collection<T2>> dependee2dependent = new HashMap<Pair<E2, C2>, Collection<T2>>(Constants
			.getNumOfMethodsInApplication());

	/**
	 * This can used to store dependent->dependee direction of dependence information. Hence, it is recommended that the
	 * subclass use this store dependence information.
	 *
	 * @invariant dependent2dependee != null
	 */
	private final Map<Pair<T1, C1>, Collection<E1>> dependent2dependee = new HashMap<Pair<T1, C1>, Collection<E1>>(Constants
			.getNumOfMethodsInApplication());

	/**
	 * This retrieves dependence from the given analysis.
	 */
	private final IDependenceRetriever<T1, C1, E1, E2, C2, T2> retriever;

	/**
	 * Creates an instance of this class.
	 *
	 * @param dependenceAnalysis for which indirect dependence info (or dependence closure) is to be provided.
	 * @param daRetriever should be used to retrieve dependence information from the given analysis.
	 * @pre dependenceAnalysis != null and daRetriever != null
	 */
	IndirectDependenceAnalysis(final IDependencyAnalysis<T1, C1, E1, E2, C2, T2> dependenceAnalysis,
			final IDependenceRetriever<T1, C1, E1, E2, C2, T2> daRetriever) {
		da = dependenceAnalysis;
		retriever = daRetriever;
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.interfaces.IAnalysis#analyze()
	 */
	@Empty public void analyze() {
		// does nothing
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.interfaces.IAnalysis#doesPreProcessing()
	 */
	@Empty public boolean doesPreProcessing() {
		return false;
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.dependency.IDependencyAnalysis#getDependees(Object, Object)
	 */
	public Collection<E1> getDependees(final T1 dependent, final C1 context) {
		final Pair<T1, C1> _key = new Pair<T1, C1>(dependent, context);
		Collection<E1> _result = dependent2dependee.get(_key);

		if (_result == null) {
			_result = new ArrayList<E1>();
			final Collection<Pair<T1, C1>> _processed = new ArrayList<Pair<T1, C1>>();
			final IWorkBag<Pair<T1, C1>> _wb = new HistoryAwareFIFOWorkBag<Pair<T1, C1>>(_processed);
			final Collection<E1> _dependees = da.getDependees(dependent, context);
			_result.addAll(_dependees);
			_wb.addAllWork(retriever.convertToConformantDependents(_dependees, dependent, context));

			while (_wb.hasWork()) {
				final Pair<T1, C1> _work = _wb.getWork();
				final Collection<E1> _dependees2 = retriever.getDependees(da, _work.getFirst(), _work.getSecond());
				_result.addAll(_dependees);
				_wb.addAllWork(retriever.convertToConformantDependents(_dependees2, _work.getFirst(), _work.getSecond()));
			}
			dependent2dependee.put(_key, _result);
		}
		return Collections.unmodifiableCollection(_result);
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.dependency.IDependencyAnalysis#getDependents(java.lang.Object, java.lang.Object)
	 */
	public Collection<T2> getDependents(final E2 dependee, final C2 context) {
		final Pair<E2, C2> _key = new Pair<E2, C2>(dependee, context);
		Collection<T2> _result = dependee2dependent.get(_key);

		if (_result == null) {
			_result = new ArrayList<T2>();
			final Collection<Pair<E2, C2>> _processed = new ArrayList<Pair<E2, C2>>();
			final IWorkBag<Pair<E2, C2>> _wb = new HistoryAwareFIFOWorkBag<Pair<E2, C2>>(_processed);
			final Collection<T2> _dependents = da.getDependents(dependee, context);
			_result.addAll(_dependents);
			_wb.addAllWork(retriever.convertToConformantDependees(_dependents, dependee, context));

			while (_wb.hasWork()) {
				final Pair<E2, C2> _work = _wb.getWork();
				final Collection<T2> _dependents2 = retriever.getDependents(da, _work.getFirst(), _work.getSecond());
				_result.addAll(_dependents2);
				_wb.addAllWork(retriever.convertToConformantDependees(_dependents2, _work.getFirst(), _work.getSecond()));
			}

			dependee2dependent.put(_key, _result);
		}
		return Collections.unmodifiableCollection(_result);
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.dependency.IDependencyAnalysis#getDirection()
	 */
	public Direction getDirection() {
		return da.getDirection();
	}

	/**
	 * @see edu.ksu.cis.indus.interfaces.IIdentification#getIds()
	 */
	public Collection<IDependencyAnalysis.DependenceSort> getIds() {
		return da.getIds();
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.dependency.IDependencyAnalysis#getIndirectVersionOfDependence()
	 */
	public IDependencyAnalysis<T1, C1, E1, E2, C2, T2> getIndirectVersionOfDependence() {
		return this;
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.interfaces.IAnalysis#getPreProcessor()
	 */
	@Empty public IProcessor getPreProcessor() {
		return null;
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.interfaces.IAnalysis#initialize(java.util.Map)
	 */
	@SuppressWarnings("unused") @Empty public void initialize(final Map<Comparable<?>, Object> infoParam)
			throws InitializationException {
		// does nothing
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.dependency.IDependencyAnalysis#isStable()
	 */
	public boolean isStable() {
		return da.isStable();
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.dependency.IDependencyAnalysis#reset()
	 */
	public void reset() {
		dependee2dependent.clear();
		dependent2dependee.clear();
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.interfaces.IAnalysis#setBasicBlockGraphManager(edu.ksu.cis.indus.common.soot.BasicBlockGraphMgr)
	 */
	@Empty public void setBasicBlockGraphManager(@SuppressWarnings("unused") final BasicBlockGraphMgr bbm) {
		// does nothing
	}

	/**
	 * Returns a stringized representation of this analysis. The representation includes the results of the analysis.
	 *
	 * @param methods for which the information needs to be stringized.
	 * @return a stringized representation of this object.
	 * @pre methods != null
	 * @post result != null
	 */
public String toString(final Collection<SootMethod> methods) {
		return "Statistics for indirect dependence as calculated by " + getClass().getName() + "\n" + MapUtils.verbosePrint(
				dependee2dependent);
	}
}

// End of File
