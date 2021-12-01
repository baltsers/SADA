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
 * This annotation indicates the degree to which a method is functional (free of observational side-effect). Parameters and
 * return values of the method are immutable independent of the degree of functional-ness.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.3 $ $Date: 2007/02/10 19:08:37 $
 */
@Target({ ElementType.METHOD }) @Documented public @interface Functional {

	/**
	 * The enumeration of various access specifiers in Java.
	 * 
	 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
	 * @author $Author: rvprasad $
	 * @version $Revision: 1.3 $ $Date: 2007/02/10 19:08:37 $
	 */
	public enum AccessSpecifier {
		/**
		 * Private access specification.
		 */
		PRIVATE,
		/**
		 * Package private access specification.
		 */
		PACKAGE,
		/**
		 * Protected access specification.
		 */
		PROTECTED,
		/**
		 * Public access specification.
		 */
		PUBLIC
	};

	/**
	 * Provides the access specification level at and beyond (less exposure) which the method is functional.
	 * 
	 * @return the access specification level.
	 */
	AccessSpecifier level() default AccessSpecifier.PRIVATE;
}

// End of File
