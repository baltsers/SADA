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

package edu.ksu.cis.indus.staticanalyses.interfaces;

import edu.ksu.cis.indus.interfaces.IActivePart;
import edu.ksu.cis.indus.processing.Context;

import java.util.Collection;

import soot.SootMethod;
import soot.Value;
import soot.jimple.InvokeExpr;

/**
 * This is the interface to be provided by an analysis that operates on values (which may be symbolic). The analysis that
 * implement this interface are behavioral analysis rather than structural analysis.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.21 $
 * @param <V> is the type of the value object.
 */
public interface IValueAnalyzer<V>
		extends IAnalyzer {

	/**
	 * The id of this interface.
	 */
	Comparable<? extends Object> ID = "value flow analyzer";

	/**
	 * The id of the tag used by the underlying flow analysis.
	 */
	String TAG_ID = "id of tag used by flow analysis";

	/**
	 * Returns the active part of this object.
	 *
	 * @return the active part.
	 */
	public IActivePart getActivePart();

	/**
	 * Returns the values associated with thrown exceptions for the given invocation expression and <code>this.context</code>.
	 *
	 * @param e is the method invoke expression.
	 * @param context in which the returned values will be associatd with the invocation site.
	 * @return the collection of values of type of the given exception class.
	 * @pre e != null and context != null
	 * @post result != null
	 */
	Collection<V> getThrownValues(InvokeExpr e, Context context);

	/**
	 * Returns the values associated with exceptions thrown by the given method in the given context..
	 *
	 * @param method of interest
	 * @param context in which the returned values will be associatd with the method.
	 * @return the collection of values of type of the given exception class.
	 * @pre method != null and context != null
	 * @post result != null
	 */
	Collection<V> getThrownValues(SootMethod method, Context context);

	/**
	 * Retrieves the values associated with the given value expression in the given context.
	 *
	 * @param value expression for which values are requested.
	 * @param context in which the returned values will be associated with the entity.
	 * @return the collection of values.
	 * @pre context != null
	 * @pre value != null
	 * @post result != null
	 */
	Collection<V> getValues(Value value, Context context);

	/**
	 * Retrieves the values associated with the given parameter position in the given context.
	 *
	 * @param paramIndex is the position of the parameter.
	 * @param context in which value is requested.
	 * @return the collection of values
	 * @pre context != null and 0 &lt;= paramIndex &lt context.getCurrentMethod().getParameterCount()
	 * @pre context.getCurrentMethod() != null
	 * @post result != null
	 */
	Collection<V> getValuesForParameter(int paramIndex, Context context);

	/**
	 * Retrieves the values associated with <code>this</code> variable in the given context.
	 *
	 * @param context in which the returned values will be associatd with <code>this</code> variable.
	 * @return the collection of values
	 * @pre context != null
	 * @pre context.getCurrentMethod() != null
	 * @post result != null
	 */
	Collection<V> getValuesForThis(Context context);
}

// End of File
