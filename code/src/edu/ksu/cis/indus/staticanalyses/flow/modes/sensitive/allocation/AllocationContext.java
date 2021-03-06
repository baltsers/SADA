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

package edu.ksu.cis.indus.staticanalyses.flow.modes.sensitive.allocation;

import edu.ksu.cis.indus.processing.Context;

/**
 * This class adds support allocation site sensitive information to be captured in a context.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.14 $
 */
public class AllocationContext
		extends Context {

	/**
	 * The allocation-site component of the context. This is relevant in the allocation-site sensitive mode of analysis. In
	 * particular, when dealing with instance fields and arrays.
	 */
	protected Object allocationSite;

	/**
	 * Creates a new AllocationContext object.
	 */
	public AllocationContext() {
		super();
	}

	/**
	 * @see edu.ksu.cis.indus.processing.Context#clone()
	 */
	@Override public AllocationContext clone() {
		return (AllocationContext) super.clone();
	}

	/**
	 * Checks if <code>o</code> is equal to this object.
	 *
	 * @param o to be checked for equality.
	 * @return <code>true</code> if <code>o</code> is equal to this object; <code>false</code>, otherwise.
	 */
	@Override public boolean equals(final Object o) {
		boolean _result = false;

		if (o != null && o instanceof AllocationContext) {
			final AllocationContext _c = (AllocationContext) o;
			_result = (this == o) || (allocationSite == _c.allocationSite);
		}
		return _result && super.equals(o);
	}

	/**
	 * Returns the allocation site in this context.
	 *
	 * @return the allocation site in this context.
	 */
	public Object getAllocationSite() {
		return allocationSite;
	}

	/**
	 * Returns the hash code of this object based on the allocation site and other context constituents.
	 *
	 * @return the hash code.
	 */
	@Override public int hashCode() {
		int _result = 17;

		if (allocationSite != null) {
			_result = 37 * _result + allocationSite.hashCode();
		}
		_result = 37 * _result + super.hashCode();
		return _result;
	}

	/**
	 * Sets the allocation site in this context.
	 *
	 * @param site the allocation site in this context.
	 * @return the allocation site previously represented by this context.
	 */
	public Object setAllocationSite(final Object site) {
		final Object _temp = allocationSite;
		allocationSite = site;

		return _temp;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override public String toString() {
		return super.toString() + "\tAllocation Site: " + allocationSite;
	}
}

// End of File
