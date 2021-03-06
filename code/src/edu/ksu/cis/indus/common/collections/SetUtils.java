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

package edu.ksu.cis.indus.common.collections;

import edu.ksu.cis.indus.annotations.Functional;
import edu.ksu.cis.indus.annotations.NonNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * This class contains static utility methods that are useful in the context of <code>java.util.Set</code> instances.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.6 $ $Date: 2007/02/10 19:08:38 $
 */
public final class SetUtils {

	// /CLOVER:OFF

	/**
	 * Creates an instance of this class.
	 */
	private SetUtils() {
		super();
	}

	/**
	 * Performs set difference on the minuend and subtrahend collections.
	 * 
	 * @param <T3> is the type of the objects in the difference set.
	 * @param <T1> is the type of the objects in the minuend collection.
	 * @param <T2> is the type of the objects in the subtrahend collection.
	 * @param col1 is the minuend collection.
	 * @param col2 is the subtrahend collection.
	 * @return the difference set.
	 */
	@NonNull @Functional public static <T3, T1 extends T3, T2 extends T3> Set<T3> difference(
			@NonNull final Collection<T1> col1, @NonNull final Collection<T2> col2) {
		final Set<T3> _r = new HashSet<T3>(col1);
		for (final Iterator<T3> _i = _r.iterator(); _i.hasNext();) {
			if (col2.contains(_i.next())) {
				_i.remove();
			}
		}
		return _r;
	}

	/**
	 * Retrieves a set creating factory.
	 * 
	 * @param <T> is the type of objects stored in the set created by the returned factory.
	 * @return a factory
	 */
	@Functional @NonNull public static <T> IFactory<Set<T>> getFactory() {
		return new IFactory<Set<T>>() {

			public Set<T> create() {
				return new HashSet<T>();
			}
		};
	}

	/**
	 * Performs set intersection operation on the two input collections.
	 * 
	 * @param <T3> is the type of the objects in the intersection set.
	 * @param <T1> is the type of the objects in the first input collection.
	 * @param <T2> is the type of the objects in the second input collection.
	 * @param col1 is an input collection.
	 * @param col2 is another input collection.
	 * @return the intersection set.
	 */
	@NonNull @Functional public static <T3, T1 extends T3, T2 extends T3> Set<T3> intersection(
			@NonNull final Collection<T1> col1, @NonNull final Collection<T2> col2) {
		final Set<T3> _result = new HashSet<T3>(CollectionUtils.maxSize(col1, col2));
		for (final T3 _t : col1) {
			if (col2.contains(_t)) {
				_result.add(_t);
			}
		}
		return _result;
	}

	/**
	 * Performs set union operation on the two input collections.
	 * 
	 * @param <T3> is the type of the objects in the union set.
	 * @param <T1> is the type of the objects in the first input collection.
	 * @param <T2> is the type of the objects in the second input collection.
	 * @param col1 is an input collection.
	 * @param col2 is another input collection.
	 * @return the union set.
	 */
	@NonNull @Functional public static <T3, T1 extends T3, T2 extends T3> Set<T3> union(@NonNull final Collection<T1> col1,
			@NonNull final Collection<T2> col2) {
		final Set<T3> _r = new HashSet<T3>(CollectionUtils.maxSize(col1, col2));
		_r.addAll(col1);
		_r.addAll(col2);
		return _r;
	}

}

// End of File
