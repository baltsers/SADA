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

import edu.ksu.cis.indus.common.datastructures.Pair;

import java.util.Collection;

/**
 * The interaface used to retrieve dependence to calculate indirect dependence from direct dependence.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.11 $ $Date: 2007/02/10 19:07:02 $
 * @param <E1> is the type of dependee object in the context of dependee-to-dependent info maintenance.
 * @param <C1> is the type of context object in the context of dependee-to-dependent info maintenance.
 * @param <T1> is the type of dependent object in the context of dependee-to-dependent info maintenance.
 * @param <T2> is the type of dependent object in the context of dependent-to-dependee info maintenance.
 * @param <C2> is the type of context object in the context of dependent-to-dependee info maintenance.
 * @param <E2> is the type of dependee object in the context of dependent-to-dependee info maintenance.
 */
public interface IDependenceRetriever<T1, C1, E1, E2, C2, T2> {

	/**
	 * Converts the given dependents to a form that they can be used as dependees.
	 * 
	 * @param dependents to be converted.
	 * @param dependee yielding the dependents.
	 * @param context in which the dependee yields the dependents.
	 * @return the collection containing the dependents in a form that they can be used dependees.
	 */
	Collection<Pair<E2, C2>> convertToConformantDependees(final Collection<T2> dependents, final E2 dependee, final C2 context);

	/**
	 * Converts the given dependees to a form that they can be used as dependents.
	 * 
	 * @param dependees to be converted.
	 * @param dependent yielding the dependees.
	 * @param context in which the dependent yields the dependees.
	 * @return the collection containing the dependees in a form that they can be used dependents.
	 */
	Collection<Pair<T1, C1>> convertToConformantDependents(final Collection<E1> dependees, final T1 dependent,
			final C1 context);

	/**
	 * Retrieves the dependees based on <code>dependence</code> from <code>da</code>.
	 * 
	 * @param da to be used retrieve dependence info
	 * @param base that serves as the basis for retrieval.
	 * @param context attached with the base.
	 * @return a collection of dependence.
	 * @pre da != null and dependence != null
	 * @post result != null
	 */
	Collection<E1> getDependees(final IDependencyAnalysis<T1, C1, E1, E2, C2, T2> da, final T1 base, final C1 context);

	/**
	 * Retrieves the dependents based on <code>dependence</code> from <code>da</code>.
	 * 
	 * @param da to be used retrieve dependence info.
	 * @param base that serves as the basis for retrieval.
	 * @param context attached with the base.
	 * @return a collection of dependence.
	 * @pre da != null and dependence != null
	 * @post result != null
	 */
	Collection<T2> getDependents(final IDependencyAnalysis<T1, C1, E1, E2, C2, T2> da, final E2 base, final C2 context);
}

// End of File
