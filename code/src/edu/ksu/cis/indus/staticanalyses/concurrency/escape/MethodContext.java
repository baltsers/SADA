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

package edu.ksu.cis.indus.staticanalyses.concurrency.escape;

import edu.ksu.cis.indus.common.datastructures.FastUnionFindElement;
import edu.ksu.cis.indus.common.datastructures.HistoryAwareLIFOWorkBag;
import edu.ksu.cis.indus.common.datastructures.IWorkBag;
import edu.ksu.cis.indus.common.datastructures.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.SootMethod;
import soot.Type;

/**
 * This class represents the method/site context as described in the techreport <a
 * href="http://www.cis.ksu.edu/santos/papers/technicalReports/SAnToS-TR2003-6.pdf">Honing the Detection of Interference and
 * Ready Dependence for Slicing Concurrent Java Programs.</a> This serves more as a container for the various alias
 * sets/equivalence classes that occur at the method interface.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.47 $
 */
final class MethodContext
		extends FastUnionFindElement<MethodContext>
		implements Cloneable {

	/**
	 * The logger used by instances of this class to log messages.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodContext.class);

	/**
	 * The alias set associated with the return value of the associated method.
	 * 
	 * @invariant AliasSet.canHaveAliasSet(method.getReturnType()) implies ret != null
	 * @invariant not AliasSet.canHaveAliasSet(method.getReturnType()) implies ret == null
	 */
	AliasSet ret;

	/**
	 * The alias set associated with the <code>this</code> variable of the associated method.
	 * 
	 * @invariant method.isStatic() implies thisAS == null
	 * @invariant not method.isStatic() implies thisAS != null
	 */
	AliasSet thisAS;

	/**
	 * The alias set associated with the exceptions thrown by the associated method.
	 * 
	 * @invariant thrown != null
	 */
	AliasSet thrown;

	/**
	 * The alias sets associated with the arguments to the associated method.
	 * 
	 * @invariant method.getParameterTypes()->forall(o | AliasSet.canHaveAliasSet(o) implies
	 *            argAliasSets.get(method.getParameterTypes().indexOf(o)) != null)
	 * @invariant method.getParameterTypes()->forall(o | not AliasSet.canHaveAliasSet(o) implies
	 *            argAliasSets.get(method.getParameterTypes().indexOf(o)) == null)
	 */
	private List<AliasSet> argAliasSets;

	/**
	 * The analysis that created this instance.
	 */
	private final EquivalenceClassBasedEscapeAnalysis ecba;

	/**
	 * This indicates if the method/invocation context reads global data.
	 */
	private boolean globalDataRead;

	/**
	 * This indicates if the method/invocation context writes global data.
	 */
	private boolean globalDataWritten;

	/**
	 * The associated method.
	 */
	private final SootMethod method;

	/**
	 * Creates a new MethodContext object.
	 * 
	 * @param sm is the method being represented by this object.
	 * @param thisASParam is the alias set corresponding to "this" variable.
	 * @param argASs is the alias sets corresponding to the arguments/parameters of the method.
	 * @param retAS is the alias set corresponding to the return value of the method.
	 * @param thrownAS is the alias set corresponding to the exceptions thrown by the method.
	 * @param analysis that created this instance.
	 * @pre sm != null and argASs != null and thrownAS != null and analysis != null
	 */
	MethodContext(final SootMethod sm, final AliasSet thisASParam, final List<AliasSet> argASs, final AliasSet retAS,
			final AliasSet thrownAS, final EquivalenceClassBasedEscapeAnalysis analysis) {
		method = sm;
		ecba = analysis;
		argAliasSets = new ArrayList<AliasSet>(argASs);

		if (EquivalenceClassBasedEscapeAnalysis.canHaveAliasSet(sm.getReturnType())) {
			ret = retAS;
		}

		if (!sm.isStatic()) {
			this.thisAS = thisASParam;
		}
		thrown = thrownAS;
	}

	/**
	 * Creates a new MethodContext object. The alias sets for the various parts of the method interface is created as
	 * required.
	 * 
	 * @param sm is the method being represented by this object.
	 * @param analysis that created this instance.
	 * @pre sm != null and analysis != null
	 */
	MethodContext(final SootMethod sm, final EquivalenceClassBasedEscapeAnalysis analysis) {
		method = sm;
		ecba = analysis;

		final int _paramCount = sm.getParameterCount();

		if (_paramCount > 0) {
			argAliasSets = new ArrayList<AliasSet>(_paramCount);

			for (int _i = 0; _i < _paramCount; _i++) {
				argAliasSets.add(AliasSet.getASForType(sm.getParameterType(_i)));
			}
		} else {
			argAliasSets = Collections.emptyList();
		}

		final Type _retType = sm.getReturnType();
		ret = AliasSet.getASForType(_retType);
		thrown = AliasSet.createAliasSet();

		if (!sm.isStatic()) {
			thisAS = AliasSet.createAliasSet();
			if (sm.isSynchronized()) {
				thisAS.setLocked();
			}
		}
	}

	/**
	 * Fixes up the field maps of the alias sets in the given map. When alias sets are cloned, the field maps are cloned.
	 * Hence, they are shallow copied. This method clones the relation between the alias sets among their clones.
	 * 
	 * @param src2clone maps an representative alias set to it's clone. This is also an out parameter that will contain new
	 *            mappings.
	 * @throws CloneNotSupportedException when <code>clone()</code> fails.
	 */
	private static void fixUpFieldMapsOfClone(final Map<AliasSet, AliasSet> src2clone) throws CloneNotSupportedException {
		final IWorkBag<AliasSet> _wb = new HistoryAwareLIFOWorkBag<AliasSet>(new HashSet<AliasSet>());

		_wb.addAllWork(src2clone.keySet());

		while (_wb.hasWork()) {
			final AliasSet _src = _wb.getWork();
			final AliasSet _clone = src2clone.get(_src);
			final Map<String, AliasSet> _srcASFieldMap = _src.getFieldMap();
			final Set<String> _srcASFields = _srcASFieldMap.keySet();
			final Iterator<String> _i = _srcASFields.iterator();
			final int _iEnd = _srcASFields.size();

			for (int _iIndex = 0; _iIndex < _iEnd; _iIndex++) {
				final String _field = _i.next();

				/*
				 * We use the representative alias set as it is possible that a field may have 2 alias sets in different
				 * contexts but the same representative alias set in both contexts. We don't do the same for clones as they
				 * are the representatives until they are unified, which happens in the following block.
				 */
				final AliasSet _srcFieldAS = _src.getASForField(_field).find();
				final AliasSet _cloneFieldAS;

				_cloneFieldAS = getCloneOf(src2clone, _srcFieldAS);
				// if the clone is different from the src only then we need to further process src.
				if (_cloneFieldAS != _srcFieldAS) {
					_wb.addWork(_srcFieldAS);
				}
				_clone.putASForField(_field, _cloneFieldAS);
			}
		}

		// Unify the clones to reflect the relation between their originators.
		for (final Iterator<AliasSet> _i = src2clone.keySet().iterator(); _i.hasNext();) {
			final AliasSet _k1 = _i.next();

			for (final Iterator<AliasSet> _j = src2clone.keySet().iterator(); _j.hasNext();) {
				final AliasSet _k2 = _j.next();

				if (_k1 != _k2 && _k1.find() == _k2.find()) {
					final AliasSet _v1 = src2clone.get(_k1);
					final AliasSet _v2 = src2clone.get(_k2);
					_v1.unifyAliasSetHelper(_v2, false);
				}
			}
		}
		return;
	}

	/**
	 * Retrieves the clone corresponding to the equivalence class of the given alias set.
	 * 
	 * @param src2clone maps alias set to alias set.
	 * @param as for which the clone is required.
	 * @return an alias set.
	 * @throws CloneNotSupportedException <i>this should not occur.</i>
	 * @pre as != null and src2clone != null
	 * @pre src2clone.oclIsKindOf(Map(AliasSet, AliasSet))
	 * @pre src2clone.keySet()->forall(o | o = o.find())
	 * @post src2clone.keySet()->forall(o | o = o.find())
	 * @post src2clone.get(as.find()) != result and result != null
	 */
	private static AliasSet getCloneOf(final Map<AliasSet, AliasSet> src2clone, final AliasSet as)
			throws CloneNotSupportedException {
		final AliasSet _repr = as.find();
		AliasSet _result = src2clone.get(_repr);

		if (_result == null) {
			_result = _repr.clone();
			src2clone.put(_repr, _result);
		}
		return _result;
	}

	/**
	 * Clones this object.
	 * 
	 * @return clone of this object.
	 * @throws CloneNotSupportedException if <code>java.lang.Object.clone()</code> fails.
	 */
	@Override public MethodContext clone() throws CloneNotSupportedException {
		MethodContext _clone = null;

		if (find() != this) {
			_clone = find().clone();
		} else {
			_clone = (MethodContext) super.clone();

			/*
			 * map from the representative to it's clone. The clone is always the representative element, but this is not true
			 * for the clonee.
			 */
			final Map<AliasSet, AliasSet> _clonee2clone = new HashMap<AliasSet, AliasSet>();
			_clone.set = null;

			if (thisAS != null) {
				_clone.thisAS = getCloneOf(_clonee2clone, thisAS);
			}
			_clone.argAliasSets = new ArrayList<AliasSet>();

			for (final Iterator<AliasSet> _i = argAliasSets.iterator(); _i.hasNext();) {
				final AliasSet _tmp = _i.next();

				if (_tmp != null) {
					_clone.argAliasSets.add(getCloneOf(_clonee2clone, _tmp));
				} else {
					_clone.argAliasSets.add(null);
				}
			}

			if (ret != null) {
				_clone.ret = getCloneOf(_clonee2clone, ret);
			}
			_clone.thrown = getCloneOf(_clonee2clone, thrown);

			MethodContext.fixUpFieldMapsOfClone(_clonee2clone);
		}
		return _clone;
	}

	/**
	 * Erases intra thread and inter-procedural reference entities.
	 */
	public void eraseIntraThreadInterProcRefEntities() {
		if (find() != this) {
			find().eraseIntraThreadInterProcRefEntities();
		} else {
			if (ret != null) {
				ret.eraseIntraThreadInterProcRefEntities();
			}

			if (thrown != null) {
				thrown.eraseIntraThreadInterProcRefEntities();
			}

			if (thisAS != null) {
				thisAS.eraseIntraThreadInterProcRefEntities();
			}

			for (final Iterator<AliasSet> _i = argAliasSets.iterator(); _i.hasNext();) {
				final AliasSet _argAS = _i.next();

				if (_argAS != null) {
					_argAS.eraseIntraThreadInterProcRefEntities();
				}
			}

			for (final Iterator<AliasSet> _j = ecba.class2aliasSet.values().iterator(); _j.hasNext();) {
				final AliasSet _as = _j.next();

				_as.eraseIntraThreadInterProcRefEntities();
			}
		}
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override public String toString() {
		return new ToStringBuilder(this).append("thrown", this.thrown).append("argAliasSets", this.argAliasSets).append(
				"ret", this.ret).append("thisAS", this.thisAS).append("method", this.method).toString();
	}

	/**
	 * Rewires the context such that it contains only representative alias sets and no the nominal(indirectional) alias sets.
	 */
	void discardReferentialAliasSets() {
		if (thrown != null) {
			thrown = thrown.find();
		}

		if (thisAS != null) {
			thisAS = thisAS.find();
		}

		if (ret != null) {
			ret = ret.find();
		}

		if (argAliasSets != null && !argAliasSets.isEmpty()) {
			final int _size = argAliasSets.size();

			for (int _i = 0; _i < _size; _i++) {
				final AliasSet _temp = argAliasSets.get(_i);

				if (_temp != null) {
					argAliasSets.set(_i, _temp.find());
				}
			}
		}
	}

	/**
	 * Retrieves the alias set in the given method context that corresponds to the given alias set in this method context.
	 * 
	 * @param ref the reference alias set that occurs in this context.
	 * @param context the context in which <code>ref</code> occurs.
	 * @return the alias set in this context and that corresponds to <code>ref</code>. This will be <code>null</code> if
	 *         there is no such alias set.
	 * @pre ref != null and context != null
	 */
	AliasSet getImageOfRefInGivenContext(final AliasSet ref, final MethodContext context) {
		AliasSet _result = null;

		final Collection<Pair<AliasSet, AliasSet>> _temp = new HashSet<Pair<AliasSet, AliasSet>>();
		final AliasSet _thisAS = getThisAS();
		final AliasSet _thisAS2 = context.getThisAS();

		if (_thisAS != null && _thisAS2 != null) {
			_temp.clear();
			_result = _thisAS.getImageOfRefUnderRoot(_thisAS2, ref, _temp);
		}

		for (int _i = argAliasSets.size() - 1; _i >= 0 && _result == null; _i--) {
			final AliasSet _paramAS = getParamAS(_i);
			final AliasSet _paramAS2 = context.getParamAS(_i);

			if (_paramAS != null && _paramAS2 != null) {
				_temp.clear();
				_result = _paramAS.getImageOfRefUnderRoot(_paramAS2, ref, _temp);
			}
		}

		if (_result == null) {
			final AliasSet _thrownAS = getThrownAS();
			final AliasSet _thrownAS2 = context.getThrownAS();

			if (_thrownAS != null && _thrownAS2 != null) {
				_temp.clear();
				_result = _thrownAS.getImageOfRefUnderRoot(_thrownAS2, ref, _temp);
			}

			if (_result == null) {
				final AliasSet _returnAS = getReturnAS();
				final AliasSet _returnAS2 = context.getReturnAS();

				if (_returnAS != null && _returnAS2 != null) {
					_temp.clear();
					_result = _returnAS.getImageOfRefUnderRoot(_returnAS2, ref, _temp);
				}
			}
		}
		return _result;
	}

	/**
	 * Retrieves the alias set corresponding to the parameter occuring at position <code>index</code> in the method
	 * interface.
	 * 
	 * @param index is the position of the parameter.
	 * @return the corresponding alias set.
	 */
	AliasSet getParamAS(final int index) {
		return find().argAliasSets.get(index);
	}

	/**
	 * Retrieves the alias set corresponding to the return value of the method.
	 * 
	 * @return the corresponding alias set.
	 */
	AliasSet getReturnAS() {
		return find().ret;
	}

	/**
	 * Retrieves the alias set corresponding to "this" variable of the method.
	 * 
	 * @return the corresponding alias set.
	 */
	AliasSet getThisAS() {
		return find().thisAS;
	}

	/**
	 * Retrieves the alias set corresponding to the exceptions thrown by the method.
	 * 
	 * @return the corresponding alias set.
	 * @post result != null
	 */
	AliasSet getThrownAS() {
		return find().thrown;
	}

	/**
	 * Marks this method as reading global data.
	 */
	void globalDataWasRead() {
		globalDataRead = true;
	}

	/**
	 * Marks this method as writing global data.
	 */
	void globalDataWasWritten() {
		globalDataWritten = true;
	}

	/**
	 * Provides information if this method reads global data.
	 * 
	 * @return <code>true</code> if global data is read by this method; <code>false</code>, otherwise.
	 */
	boolean isGlobalDataRead() {
		return globalDataRead;
	}

	/**
	 * Provides information if this method writes global data.
	 * 
	 * @return <code>true</code> if global data is written by this method; <code>false</code>, otherwise.
	 */
	boolean isGlobalDataWritten() {
		return globalDataWritten;
	}

	/**
	 * Marks all reachable alias sets as being crossing thread boundary, i.e, visible in multiple threads..
	 */
	void markAsCrossingThreadBoundary() {
		if (find() != this) {
			find().markAsCrossingThreadBoundary();
		} else {
			if (ret != null) {
				ret.markAsCrossingThreadBoundary();
			}

			if (thrown != null) {
				thrown.markAsCrossingThreadBoundary();
			}

			if (thisAS != null) {
				thisAS.markAsCrossingThreadBoundary();
			}

			for (final Iterator<AliasSet> _i = argAliasSets.iterator(); _i.hasNext();) {
				final AliasSet _argAS = _i.next();

				if (_argAS != null) {
					_argAS.markAsCrossingThreadBoundary();
				}
			}
		}
	}

	/**
	 * Propogates the information from the source (this) context to the destination context. Please refer to the {@link
	 * #unifyMethodContext(MethodContext) unify(MethodContext)} for important information.
	 * 
	 * @param to is the destination of the information transfer.
	 * @pre to != null
	 */
	void propogateInfoFromTo(final MethodContext to) {
		final MethodContext _fromRep = find();
		final MethodContext _toRep = to.find();

		if (_fromRep == _toRep) {
			return;
		}

		final int _paramCount = _fromRep.method.getParameterCount();

		for (int _i = 0; _i < _paramCount; _i++) {
			if (EquivalenceClassBasedEscapeAnalysis.canHaveAliasSet(_fromRep.method.getParameterType(_i))) {
				final AliasSet _temp1 = argAliasSets.get(_i);

				if (_temp1 != null) {
					/*
					 * NULL-ARGUMENT SCENARIO: We check if the argument at the position _i was a null constant when this
					 * method context was created. If so, then we decided not propagate the effect. For this reason, the
					 * reference to the original method context should be retained and the propagation should be considered to
					 * be directional.
					 */
					final AliasSet _temp2 = _toRep.argAliasSets.get(_i);
					final AliasSet _temp3 = _fromRep.argAliasSets.get(_i);

					if (_temp3 != null && _temp2 != null) {
						_temp3.propogateInfoFromTo(_temp2);
					}
				}
			}
		}

		final AliasSet _retAS = _fromRep.ret;

		if (_retAS != null) {
			_retAS.propogateInfoFromTo(_toRep.ret);
		}
		_fromRep.thrown.propogateInfoFromTo(_toRep.thrown);

		final AliasSet _thisAS = _fromRep.thisAS;

		if (_thisAS != null) {
			_thisAS.propogateInfoFromTo(_toRep.thisAS);
		}
	}

	/**
	 * Unifies this object with itself. This is required while dealing with call-sites which may be executed multiple times.
	 */
	void selfUnify() {
		final MethodContext _methodContext = find();
		final int _paramCount = method.getParameterCount();

		for (int _i = 0; _i < _paramCount; _i++) {
			if (EquivalenceClassBasedEscapeAnalysis.canHaveAliasSet(method.getParameterType(_i))) {
				final AliasSet _aliasSet = _methodContext.argAliasSets.get(_i);

				// it is possible that the argument at a site-context is null
				if (_aliasSet != null) {
					AliasSet.selfUnify(_aliasSet);
				}
			}
		}

		final AliasSet _mRet = _methodContext.ret;

		if (_mRet != null) {
			AliasSet.selfUnify(_mRet);
		}
		AliasSet.selfUnify(_methodContext.thrown);

		final AliasSet _mThis = _methodContext.thisAS;

		if (_mThis != null) {
			AliasSet.selfUnify(_mThis);
		}
	}

	/**
	 * Unifies this context with the given context.
	 * <p>
	 * There is asymmetry in alias sets in case of contexts. It is possible that this context may not have alias set for an
	 * argument location where as <code>p</code> may have one. This happens in cases where this context corresponds to a
	 * site-context where <code>null</code> is being passed, hence, that arg position does not require an alias set whereas
	 * <code>p</code> being the context of the method being called requires alias set as there may be sites where the
	 * non-null argument may be provided. In such cases, we safely warn and continue. However, this cannot happen for thrown
	 * exception, this, or return references. associated with call sites and methods.
	 * </p>
	 * 
	 * @param p is the context with which the unification should occur.
	 */
	void unifyMethodContext(final MethodContext p) {
		if (p == null) {
			LOGGER.error("Unification with null requested.");
			throw new IllegalArgumentException("Unification with null requested.");
		}

		final MethodContext _m = find();
		final MethodContext _n = p.find();

		if (_m != _n) {
			_m.union(_n);

			final MethodContext _representative = _m.find();
			final MethodContext _represented;

			if (_representative == _m) {
				_represented = _n;
			} else {
				_represented = _m;
			}
			_representative.globalDataWritten |= _represented.globalDataWritten;
			_representative.globalDataRead |= _represented.globalDataRead;

			final int _paramCount = method.getParameterCount();

			for (int _i = 0; _i < _paramCount; _i++) {
				if (EquivalenceClassBasedEscapeAnalysis.canHaveAliasSet(method.getParameterType(_i))) {
					final AliasSet _mAS = _representative.argAliasSets.get(_i);
					final AliasSet _nAS = _represented.argAliasSets.get(_i);
					if (_mAS == null) {
						_representative.argAliasSets.set(_i, _nAS);
					} else {
						_mAS.unifyAliasSet(_nAS);
					}
				}
			}
			unifyAliasSets(_representative.ret, _represented.ret);
			unifyAliasSets(_representative.thrown, _represented.thrown);
			unifyAliasSets(_representative.thisAS, _represented.thisAS);
		}
	}

	/**
	 * Unifies the given alias sets.
	 * 
	 * @param representative is one of the alias set to be unified.
	 * @param represented is the other alias set to be unified.
	 * @pre representative != null and represented != null
	 */
	private void unifyAliasSets(final AliasSet representative, final AliasSet represented) {
		if ((representative == null && represented != null) || (representative != null && represented == null)) {
			LOGGER.error("Incompatible method contexts being unified - representative - " + representative
					+ "\n represented - " + represented);
			throw new IllegalStateException("Unifying null aliasSet");
		} else if (representative != null) {
			representative.unifyAliasSet(represented);
		}
	}
}

// End of File
