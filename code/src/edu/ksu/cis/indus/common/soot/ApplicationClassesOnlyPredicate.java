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
import edu.ksu.cis.indus.common.collections.IPredicate;

import soot.SootMethod;

/**
 * This predicate can be used to check if a given class is or method belongs to an application class.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.7 $ $Date: 2007/02/10 19:08:36 $
 */
public class ApplicationClassesOnlyPredicate
		implements IPredicate<SootMethod> {

	/**
	 * Creates an instance of this class.
	 */
	@Empty public ApplicationClassesOnlyPredicate() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean evaluate(final SootMethod object) {
		return object.getDeclaringClass().isApplicationClass();
	}
}

// End of File
