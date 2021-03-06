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

package edu.ksu.cis.indus.common.datastructures;

import edu.ksu.cis.indus.annotations.Immutable;
import edu.ksu.cis.indus.annotations.NonNull;

import java.util.Collection;

/**
 * This is a Last-in-First-out implementation of the workbag that can remember previous work pieces put into it.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.8 $ $Date: 2007/02/10 19:08:38 $
 * @param <T> The type of work handled by this work bag.
 */
public final class HistoryAwareLIFOWorkBag<T>
		extends AbstractHistoryAwareWorkBag<T> {

	/**
	 * Creates a new LIFOWorkBag object.
	 * 
	 * @param processed is the collection to be used to remember work pieces put into the bag. Refer to
	 *            <code>AbstractHistoryAwareWorkBag#AbstractHistoryAwareWorkBag(Collection)</code>.
	 */
	public HistoryAwareLIFOWorkBag(@NonNull final Collection<T> processed) {
		super(processed);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override protected void subAddWork(@NonNull @Immutable final T o) {
		container.add(0, o);
		updateInternal(o);
	}
}

// End of File
