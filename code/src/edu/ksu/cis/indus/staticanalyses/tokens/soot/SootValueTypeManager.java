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

package edu.ksu.cis.indus.staticanalyses.tokens.soot;

import edu.ksu.cis.indus.common.collections.CollectionUtils;
import edu.ksu.cis.indus.common.collections.IPredicate;
import edu.ksu.cis.indus.common.collections.InstanceOfPredicate;
import edu.ksu.cis.indus.common.soot.Constants;
import edu.ksu.cis.indus.common.soot.Util;
import edu.ksu.cis.indus.staticanalyses.tokens.IDynamicTokenTypeRelationDetector;
import edu.ksu.cis.indus.staticanalyses.tokens.IType;
import edu.ksu.cis.indus.staticanalyses.tokens.ITypeManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Set;

import soot.ArrayType;
import soot.NullType;
import soot.RefLikeType;
import soot.RefType;
import soot.SootClass;
import soot.Type;
import soot.Value;

/**
 * This class manages Soot value types and the corresponding types in user's type system.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.8 $ $Date: 2007/02/10 19:07:31 $
 */
public class SootValueTypeManager
		extends Observable
		implements ITypeManager<Type, Value> {

	/**
	 * This is a dummy implementation of <code>IType</code>.
	 * 
	 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
	 * @author $Author: rvprasad $
	 * @version $Revision: 1.8 $ $Date: 2007/02/10 19:07:31 $
	 */
	static class DummyType
			implements IType {

		/**
		 * The soot type represented by this instance.
		 */
		final Type sootType;

		/**
		 * Creates an instance of this class.
		 * 
		 * @param sType is the represented Soot type.
		 * @pre sType != null
		 */
		public DummyType(final Type sType) {
			sootType = sType;
		}
	}

	/**
	 * This is the dynamic token-type relation evaluator.
	 */
	private final IDynamicTokenTypeRelationDetector<Value> evaluator;

	/**
	 * This maps soot types to user's type.
	 */
	private final Map<Type, IType> sootType2Type;

	/**
	 * The type corresponding to the soot type representing <code>java.lang.Object</code>.
	 */
	private IType tokenTypeForObjectType;

	/**
	 * This predicate is used to detect types that can hold null constant.
	 */
	private final IPredicate<Type> typesForNullConstPredicate;

	/**
	 * Creates an instance of this class.
	 */
	public SootValueTypeManager() {
		super();
		typesForNullConstPredicate = new InstanceOfPredicate<RefLikeType, Type>(RefLikeType.class);
		evaluator = new SootDynamicTokenTypeRelationDetector();
		sootType2Type = new HashMap<Type, IType>(Constants.getNumOfClassesInApplication());
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<IType> getAllTypes(final Value value) {
		final Value _theValue = value;
		final Type _type = _theValue.getType();
		final Collection<IType> _result = new ArrayList<IType>();

		_result.add(getTokenTypeForRepType(_type));

		if (_type instanceof RefType) {
			for (final Iterator _i = Util.getAncestors(((RefType) _type).getSootClass()).iterator(); _i.hasNext();) {
				_result.add(getTokenTypeForRepType(((SootClass) _i.next()).getType()));
			}
		}

		if (_type instanceof NullType) {
			final Collection<Type> _s = CollectionUtils.collect(sootType2Type.keySet(), typesForNullConstPredicate);

			for (final Iterator<Type> _i = _s.iterator(); _i.hasNext();) {
				final Type _t = _i.next();
				_result.add(sootType2Type.get(_t));
			}
		} else if (_type instanceof ArrayType) {
			final IType _t = getTokenTypeForObjectType();

			if (_t != null) {
				_result.add(_t);
			}
		}

		return _result;
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.tokens.ITypeManager#getDynamicTokenTypeRelationEvaluator()
	 */
	public IDynamicTokenTypeRelationDetector<Value> getDynamicTokenTypeRelationEvaluator() {
		return evaluator;
	}

	/**
	 * {@inheritDoc}
	 */
	public IType getExactType(final Value value) {
		return getTokenTypeForRepType(value.getType());
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.tokens.ITypeManager#getTokenTypeForRepType(Object)
	 */
	public IType getTokenTypeForRepType(final Type sootType) {
		IType _result = sootType2Type.get(sootType);

		if (_result == null) {
			_result = new DummyType(sootType);
			sootType2Type.put(sootType, _result);
			setChanged();
			notifyObservers(new NewTypeCreated(_result));
		}
		return _result;
	}

	/**
	 * Resets the manager.
	 */
	public void reset() {
		sootType2Type.clear();
		evaluator.reset();
		tokenTypeForObjectType = null;
	}

	/**
	 * Retrieves the type for the soot type representing <code>java.lang.Object</code>.
	 * 
	 * @return the type
	 */
	private IType getTokenTypeForObjectType() {
		if (tokenTypeForObjectType == null) {
			final Set<Map.Entry<Type, IType>> _entrySet = sootType2Type.entrySet();
			final Iterator<Map.Entry<Type, IType>> _i = _entrySet.iterator();
			final int _iEnd = _entrySet.size();

			for (int _iIndex = 0; _iIndex < _iEnd; _iIndex++) {
				final Map.Entry<Type, IType> _entry = _i.next();
				final Type _sootType = _entry.getKey();

				if (_sootType instanceof RefType) {
					final SootClass _sc = ((RefType) _sootType).getSootClass();

					if (_sc.getName().equals("java.lang.Object")) {
						tokenTypeForObjectType = _entry.getValue();
						break;
					}
				}
			}
		}
		return tokenTypeForObjectType;
	}
}

// End of File
