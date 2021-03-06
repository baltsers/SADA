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

import edu.ksu.cis.indus.annotations.Empty;
import edu.ksu.cis.indus.annotations.Functional;
import edu.ksu.cis.indus.annotations.Immutable;
import edu.ksu.cis.indus.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides the basic implementation for elements to be used in fast-union-find algorithm as defined by Aho,
 * Ullman, and Sethi in the "Dragon" book.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.11 $ $Date: 2007/02/10 19:08:38 $
 * @param <T> the type parameter for this class.
 */
public class FastUnionFindElement<T extends FastUnionFindElement<T>> {

	/**
	 * A sequence of children elements of this element.
	 */
	protected List<T> children;

	/**
	 * This is the set to which this element belongs to.
	 */
	protected T set;

	/**
	 * This is the type associated with this element.
	 */
	protected Object type;

	/**
	 * Creates an instance of this class.
	 */
	@Empty public FastUnionFindElement() {
		// does nothing
	}

	/**
	 * Adds a new child to this element.
	 * 
	 * @param child to be added.
	 */
	public final void addChild(@NonNull @Immutable final T child) {
		if (children == null) {
			children = new ArrayList<T>();
		}
		children.add(child);
	}

	/**
	 * Retrieves the element that represents the equivalence class to which this element belongs to.
	 * 
	 * @return the representative element.
	 */
	@NonNull public final T find() {
		@SuppressWarnings("unchecked") T _result = (T) this;

		while (_result.set != null) {
			_result = _result.set;
		}

		if (_result != this) {
			set = _result;
		}
		return _result;
	}

	/**
	 * Retrieves the type of this element.
	 * 
	 * @return the type of this element.
	 */
	@Functional public final Object getType() {
		return find().type;
	}

	/**
	 * Checks if this element has any children.
	 * 
	 * @return <code>true</code> if it has children; <code>false</code>, otherwise.
	 */
	@Functional public final boolean isAtomic() {
		return children == null || children.size() == 0;
	}

	/**
	 * Checks if this element is bound to a type.
	 * 
	 * @return <code>true</code> if it is bound to a type; <code>false</code>, otherwise.
	 */
	@Functional public final boolean isBound() {
		return find().type != null;
	}

	/**
	 * Checks if the given element and this element represent the same type. As this implementation does not deal with types,
	 * it assumes no two element belong to the same type, hence, always returns <code>false</code>.
	 * 
	 * @param e is the element to checked for equivalence.
	 * @return <code>false</code>
	 */
	@Functional public final boolean sameType(@NonNull @Immutable final T e) {
		boolean _result = false;

		if (e.type != null && type != null) {
			_result = e.type.equals(type);
		}
		return _result;
	}

	/**
	 * Set the type of the element.
	 * 
	 * @param theType of the element.
	 * @throws IllegalStateException when the type of the element is already set.
	 */
	public final void setType(@NonNull @Immutable final Object theType) {
		if (set == null) {
			if (type == null) {
				type = theType;
			} else {
				throw new IllegalStateException("Cannot set a type on an element with a fixed type.");
			}
		} else {
			find().setType(theType);
		}
	}

	/**
	 * Unifies the given element with this element.
	 * 
	 * @param e is the element to be unified with this element.
	 * @return <code>true</code> if this element was unified with the given element; <code>false</code>, otherwise.
	 */
	public final boolean unify(@NonNull final T e) {
		boolean _result = false;
		final T _a = find();
		final T _b = e.find();

		if (_a == _b || _a.sameType(_b)) {
			_result = true;
		} else if (!(_a.isAtomic() || _b.isAtomic())) {
			_a.union(_b);
			_result = _a.unifyChildren(_b);
		} else if (!(_a.isBound() && _b.isBound())) {
			_a.union(_b);
			_result = true;
		}

		return _result;
	}

	/**
	 * Unifies the children of this element with that of the given element.
	 * 
	 * @param e is the element whose children needs to be unified with that of this element.
	 * @return <code>true</code>, if the tree rooted at this element and at <code>e</code> was unified successfully;
	 *         <code>false</code>, otherwise.
	 */
	public final boolean unifyChildren(@NonNull final T e) {
		boolean _result = false;

		if (children != null && e.children != null && children.size() == e.children.size()) {
			_result = true;

			for (int _i = children.size() - 1; _i >= 0 && _result; _i--) {
				final T _c1 = children.get(_i);
				final T _c2 = e.children.get(_i);
				_result &= _c1.unify(_c2);
			}
		}
		return _result;
	}

	/**
	 * Merges the equivalence classes containing <code>e</code> and this element.
	 * 
	 * @param e is the element in an equivalence class that needs to be merged.
	 */
	public final void union(@NonNull final FastUnionFindElement<T> e) {
		final T _a = find();
		final T _b = e.find();

		if (_a != _b) {
			if (_b.isBound()) {
				_a.set = _b;
			} else {
				// if a.isBound() or neither is bound
				_b.set = _a;
			}
		}
	}
}

// End of File
