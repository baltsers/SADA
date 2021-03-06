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
import edu.ksu.cis.indus.common.datastructures.Pair;

import java.util.Collection;
import java.util.HashSet;

/**
 * This implementation of <code>IDependenceRetriever</code> is used in instance when the dependence information is provided
 * in terms of statements and method pair. Use this in cases such as ready dependence.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.7 $ $Date: 2007/02/10 19:07:01 $
 * @param <E1> is the type of dependee object in the context of dependee-to-dependent info maintenance.
 * @param <C1> is the type of context object in the context of dependee-to-dependent info maintenance.
 * @param <T1> is the type of dependent object in the context of dependee-to-dependent info maintenance.
 * @param <T2> is the type of dependent object in the context of dependent-to-dependee info maintenance.
 * @param <C2> is the type of context object in the context of dependent-to-dependee info maintenance.
 * @param <E2> is the type of dependee object in the context of dependent-to-dependee info maintenance.
 */
final class PairRetriever<T1, C1, E1, E2, C2, T2>
		extends AbstractDependenceRetriever<T1, C1, Pair<E1, C1>, E2, C2, Pair<T2, C2>> {

	/**
	 * Creates an instance of this class.
	 */
	@Empty public PairRetriever() {
		// does nothing
	}

	/**
	 * @see IDependenceRetriever#convertToConformantDependees(java.util.Collection, java.lang.Object, java.lang.Object)
	 */
	public Collection<Pair<E2, C2>> convertToConformantDependees(Collection<Pair<T2, C2>> dependents, final E2 base,
			final C2 context) {
		final Collection<Pair<E2, C2>> _result = new HashSet<Pair<E2, C2>>();
		for (final Pair<T2, C2> _pair : dependents) {
			_result.add((Pair<E2, C2>) _pair);
		}
		return _result;
	}

	/**
	 * @see IDependenceRetriever#convertToConformantDependents(java.util.Collection, java.lang.Object, java.lang.Object)
	 */
	public Collection<Pair<T1, C1>> convertToConformantDependents(final Collection<Pair<E1, C1>> dependees, final T1 base,
			final C1 context) {
		final Collection<Pair<T1, C1>> _result = new HashSet<Pair<T1, C1>>();
		for (final Pair<E1, C1> _pair : dependees) {
			_result.add((Pair<T1, C1>) _pair);
		}
		return _result;
	}
}

// End of File
