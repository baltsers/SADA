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

package edu.ksu.cis.indus.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * This annotation indicates the numerical partition to which a number belongs to. When used on methods, it applies to the
 * return value of the method.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.4 $ $Date: 2007/02/10 19:08:37 $
 */
@Target({ ElementType.LOCAL_VARIABLE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER }) @Documented public @interface NumericalConstraint {

	/**
	 * The numerical value partition type.
	 * 
	 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
	 * @author $Author: rvprasad $
	 * @version $Revision: 1.4 $ $Date: 2007/02/10 19:08:37 $
	 */
	public enum NumericalValue {

		/**
		 * Negative (&lt; 0) value.
		 */
		NEGATIVE,

		/**
		 * Non-negative (&gt; -1) value.
		 */
		NON_NEGATIVE,

		/**
		 * Non-positive (&lt; 1) value.
		 */
		NON_POSITIVE,

		/**
		 * Positive (&gt; 0) value.
		 */
		POSITIVE,

		/**
		 * Zero value.
		 */
		ZERO
	}

	/**
	 * Provides a value partition to which the annotated numerical entity belongs to.
	 * 
	 * @return a partition.
	 */
	NumericalValue value();

}

// End of File
