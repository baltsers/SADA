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

import edu.ksu.cis.indus.common.collections.IPredicate;
import edu.ksu.cis.indus.common.collections.InstanceOfPredicate;
import edu.ksu.cis.indus.common.collections.SetUtils;
import edu.ksu.cis.indus.common.datastructures.Pair;
import edu.ksu.cis.indus.interfaces.IUseDefInfo;
import edu.ksu.cis.indus.staticanalyses.InitializationException;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.SootMethod;
import soot.jimple.AssignStmt;

/**
 * This class provides data dependence information which considers references. Hence, it considers the effects of aliasing. It
 * is an adapter for an interprocedural use-def analysis which considers the effects of aliasing. It can be configured to
 * provide dependence based on static field references.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.42 $
 */
public class ReferenceBasedDataDA
		extends
		AbstractDependencyAnalysis<AssignStmt, SootMethod, Pair<AssignStmt, SootMethod>, Object, Map<Pair<AssignStmt, SootMethod>, Collection<Pair<AssignStmt, SootMethod>>>, AssignStmt, SootMethod, Pair<AssignStmt, SootMethod>, Object, Map<Pair<AssignStmt, SootMethod>, Collection<Pair<AssignStmt, SootMethod>>>> {

	/**
	 * This predicate can be used to check if an object of this class type.
	 */
	public static final IPredicate<IDependencyAnalysis<?, ?, ?, ?, ?, ?>> INSTANCEOF_PREDICATE = new InstanceOfPredicate<ReferenceBasedDataDA, IDependencyAnalysis<?, ?, ?, ?, ?, ?>>(
			ReferenceBasedDataDA.class);

	/**
	 * The logger used by instances of this class to log messages.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceBasedDataDA.class);

	/**
	 * This provides inter-procedural use-def information which considers the effects of aliasing.
	 */
	private IUseDefInfo<Pair<AssignStmt, SootMethod>, Pair<AssignStmt, SootMethod>> aliasedUD;

	/**
	 * This provides static field use-def information.
	 */
	private IUseDefInfo<Pair<AssignStmt, SootMethod>, Pair<AssignStmt, SootMethod>> staticFieldRefUD;

	/**
	 * Creates an instance of this class.
	 */
	public ReferenceBasedDataDA() {
		super(Direction.BI_DIRECTIONAL);
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.interfaces.AbstractAnalysis#analyze()
	 */
	@Override public void analyze() {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("BEGIN: Reference Based Data Dependence processing");
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("ReferenceBasedDataDA.analyze() - " + toString());
		}

		if (aliasedUD.isStable()) {
			stable();
		} else {
			unstable();
		}

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("END: Reference Based Data Dependence processing");
		}
	}

	/**
	 * Return the statements on which field/array access in <code>stmt</code> in <code>method</code> depends on.
	 * 
	 * @param stmt in which aliased data is read.
	 * @param method in which <code>stmt</code> occurs.
	 * @return a collection of statements which affect the data being read in <code>stmt</code>.
	 * @post result->forall(o | o.getFirst().getLeftOf().oclIsKindOf(FieldRef) or
	 *       o.getFirst().getLeftOf().oclIsKindOf(ArrayRef))
	 * @see edu.ksu.cis.indus.staticanalyses.dependency.AbstractDependencyAnalysis#getDependees(java.lang.Object,
	 *      java.lang.Object)
	 */
	public Collection<Pair<AssignStmt, SootMethod>> getDependees(final AssignStmt stmt, final SootMethod method) {
		final Collection<Pair<AssignStmt, SootMethod>> _result;

		if (stmt.containsArrayRef() || stmt.containsFieldRef()) {
			if (staticFieldRefUD != null) {
				_result = SetUtils.union(aliasedUD.getDefs(stmt, method), staticFieldRefUD.getDefs(stmt, method));
			} else {
				_result = aliasedUD.getDefs(stmt, method);
			}
		} else {
			_result = Collections.emptyList();
		}

		return _result;
	}

	/**
	 * Return the statements which depend on the field/array access in <code>stmt</code> in <code>method</code>.
	 * 
	 * @param stmt in which aliased data is written.
	 * @param method in which <code>stmt</code> occurs.
	 * @return a collection of statements which are affectted by the data write in <code>stmt</code>.
	 * @post result->forall(o | o.getFirst().getRightOp().oclIsKindOf(FieldRef) or
	 *       o.getFirst().getRightOp().oclIsKindOf(ArrayRef))
	 * @see edu.ksu.cis.indus.staticanalyses.dependency.AbstractDependencyAnalysis#getDependents(java.lang.Object,
	 *      java.lang.Object)
	 */
	public Collection<Pair<AssignStmt, SootMethod>> getDependents(final AssignStmt stmt, final SootMethod method) {
		final Collection<Pair<AssignStmt, SootMethod>> _result;

		if (stmt.containsArrayRef() || stmt.containsFieldRef()) {
			if (staticFieldRefUD != null) {
				_result = SetUtils.union(aliasedUD.getUses(stmt, method), staticFieldRefUD.getUses(stmt, method));
			} else {
				_result = aliasedUD.getUses(stmt, method);
			}
		} else {
			_result = Collections.emptyList();
		}

		return _result;
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.dependency.IDependencyAnalysis#getIds()
	 */
	public Collection<IDependencyAnalysis.DependenceSort> getIds() {
		return Collections.singleton(IDependencyAnalysis.DependenceSort.REFERENCE_BASED_DATA_DA);
	}

	// /CLOVER:OFF

	/**
	 * Returns a stringized representation of this analysis. The representation includes the results of the analysis.
	 * 
	 * @return a stringized representation of this object.
	 */
	@Override public String toString() {
		return aliasedUD + "\n" + staticFieldRefUD;
	}

	// /CLOVER:ON

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.dependency.AbstractDependencyAnalysis#getDependenceRetriever()
	 */
	@Override protected IDependenceRetriever<AssignStmt, SootMethod, Pair<AssignStmt, SootMethod>, AssignStmt, SootMethod, Pair<AssignStmt, SootMethod>> getDependenceRetriever() {
		return new PairRetriever<AssignStmt, SootMethod, AssignStmt, AssignStmt, SootMethod, AssignStmt>();
	}

	/**
	 * Extracts information provided by environment at initialization time. The user can configure this analysis to include
	 * static field reference based dependence information by passing in an implementation of <code>IUseDefInfo</code>
	 * implementation mapped to <code>IUseDefInfo.GLOBAL_USE_DEF_ID</code> constant in the information map.
	 * 
	 * @throws InitializationException if an implementation that provides aliased interprocedural use-def information is not
	 *             provided.
	 * @pre info.get(IUseDefInfo.ALIASED_USE_DEF_ID) != null
	 * @see edu.ksu.cis.indus.staticanalyses.interfaces.AbstractAnalysis#setup()
	 */
	@Override protected void setup() throws InitializationException {
		super.setup();

		aliasedUD = (IUseDefInfo) info.get(IUseDefInfo.ALIASED_USE_DEF_ID);

		if (aliasedUD == null) {
			throw new InitializationException(IUseDefInfo.ALIASED_USE_DEF_ID + " was not provided.");
		}

		staticFieldRefUD = (IUseDefInfo) info.get(IUseDefInfo.GLOBAL_USE_DEF_ID);

		if (staticFieldRefUD == null) {
			LOGGER.info(IUseDefInfo.GLOBAL_USE_DEF_ID + " was not provided.  Hence, static field reference based dependence"
					+ " info will not be provided.");
		}
	}
}

// End of File
