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

package edu.ksu.cis.indus.staticanalyses.flow;

import edu.ksu.cis.indus.processing.Context;
import edu.ksu.cis.indus.staticanalyses.tokens.ITokens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import soot.Local;
import soot.RefType;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.JimpleBody;

/**
 * An abstract implementation of <code>IMethodVariant</code> with the most general content in place.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.13 $ $Date: 2007/02/10 19:07:01 $
 * @param <SYM> is the type of symbol whose flow is being analyzed.
 * @param <T> is the type of the token set object.
 * @param <N> is the type of the summary node in the flow analysis.
 */
public abstract class AbstractMethodVariant<SYM, T extends ITokens<T, SYM>, N extends IFGNode<SYM, T, N>, R>
		implements IMethodVariant<N> {

	/**
	 * The instance of <code>FA</code> which was responsible for the creation of this variant.
	 * 
	 * @invariant fa != null
	 */
	protected final FA<SYM, T, N, R> fa;

	/**
	 * The flow graph node associated with an abstract single return point of the corresponding method. This will be
	 * <code>null</code>, if the associated method's return type is any non-ref type.
	 * 
	 * @invariant _method.getReturnType().oclIsKindOf(RefLikeType) implies returnVar != null
	 * @invariant not _method.getReturnType().oclIsKindOf(RefLikeType) implies returnVar == null
	 */
	protected final N returnVar;

	/**
	 * The flow graph nodes associated with the this variable of the corresponding method. This will be <code>null</code>,
	 * if the associated method is <code>static</code>.
	 * 
	 * @invariant _method.isStatic() implies thisVar == null
	 * @invariant not _method.isStatic() implies thisVar != null
	 */
	protected final N thisVar;

	/**
	 * The flow graph node associated with an abstract single exceptional return point of the corresponding method.
	 */
	protected final N thrownNode;

	/**
	 * The statement visitor used to process in the statement in the correpsonding method.
	 * 
	 * @invariant stmt != null
	 */
	protected final IStmtSwitch stmt;

	/**
	 * The manager of AST node variants. This is required as in Jimple, the same AST node instance may occur at different
	 * locations in the AST as it serves the purpose of AST representation.
	 * 
	 * @invariant astvm != null
	 */
	protected final IVariantManager<ValuedVariant<N>, Value> astvm;

	/**
	 * The method represented by this variant.
	 * 
	 * @invariant method != null
	 */
	protected final SootMethod method;

	/**
	 * The array of flow graph nodes associated with the parameters of thec corresponding method.
	 * 
	 * @invariant parameters.oclIsKindOf(Sequence(IFGNode))
	 * @invariant _method.getParameterCount() == 0 implies parameters == null
	 * @invariant _method.getParameterTypes()->forall(p | p.oclIsKindOf(RefLikeType) implies
	 *            parameters.at(method.getParameterTypes().indexOf(p)) != null)
	 * @invariant _method.getParameterTypes()->forall(p | not p.oclIsKindOf(RefLikeType) implies
	 *            parameters.at(method.getParameterTypes().indexOf(p)) == null)
	 */
	protected final List<N> parameters;

	/**
	 * The context which resulted in the creation of this variant.
	 * 
	 * @invariant context != null
	 */
	private final Context context;

	/**
	 * Creates an instance of this class.
	 * 
	 * @param sm is the method be represented.
	 * @param astVariantManager to be used for the AST chunks of the represented method.
	 * @param theFA the instance of the flow framework with which this variant operates.
	 * @pre sm != null and astVariantManager != null and theFA != null
	 */
	protected AbstractMethodVariant(final SootMethod sm, final IVariantManager<ValuedVariant<N>, Value> astVariantManager,
			final FA<SYM, T, N, R> theFA) {
		super();
		method = sm;
		astvm = astVariantManager;
		fa = theFA;
		context = fa.getAnalyzer().getContext().clone();
		context.callNewMethod(sm);

		fa.processClass(sm.getDeclaringClass());
		sm.addTag(fa.getTag());

		final Collection<Type> _typesToProcess = new HashSet<Type>();
		final RefType _sootType = sm.getDeclaringClass().getType();

		if (!sm.isStatic() && shouldConsider(_sootType)) {
			thisVar = fa.getNewFGNode();
			_typesToProcess.add(_sootType);
		} else {
			thisVar = null;
		}

		final int _pCount = sm.getParameterCount();
		parameters = new ArrayList<N>(_pCount);

		for (int _i = 0; _i < _pCount; _i++) {
			if (shouldConsider(sm.getParameterType(_i))) {
				parameters.add(fa.getNewFGNode());
				_typesToProcess.add(sm.getParameterType(_i));
			} else {
				parameters.add(null);
			}
		}

		if (shouldConsider(sm.getReturnType())) {
			returnVar = fa.getNewFGNode();
			_typesToProcess.add(sm.getReturnType());
		} else {
			returnVar = null;
		}

		thrownNode = fa.getNewFGNode();

		stmt = fa.getStmt(this);

		if (method.isConcrete()) {
			final JimpleBody _jb = (JimpleBody) method.retrieveActiveBody();

			for (final Iterator<Local> _i = _jb.getLocals().iterator(); _i.hasNext();) {
				final Type _localType = _i.next().getType();

				if (shouldConsider(_localType)) {
					fa.processType(_localType);
				}
			}
		}

		for (final Iterator<Type> _i = _typesToProcess.iterator(); _i.hasNext();) {
			fa.processType(_i.next());
		}
	}

	/**
	 * @see IMethodVariant#getASTNode(Value, Context)
	 */
	public final N getASTNode(final Value v, final Context c) {
		return getASTVariant(v, c).getFGNode();
	}

	/**
	 * @see IMethodVariant#getASTVariant(Value, Context)
	 */
	public final ValuedVariant<N> getASTVariant(final Value v, final Context ctxt) {
		return astvm.select(v, ctxt);
	}

	/**
	 * @see IMethodVariant#getContext()
	 */
	public final Context getContext() {
		return context;
	}

	/**
	 * @see IMethodVariant#getFA()
	 */
	public final FA<?, ?, N, ?> getFA() {
		return fa;
	}

	/**
	 * @see IMethodVariant#getMethod()
	 */
	public final SootMethod getMethod() {
		return method;
	}

	/**
	 * @see IMethodVariant#queryASTNode(Value, Context)
	 */
	public final N queryASTNode(final Value v, final Context c) {
		final ValuedVariant<N> _var = queryASTVariant(v, c);
		N _temp = null;

		if (_var != null) {
			_temp = _var.getFGNode();
		}
		return _temp;
	}

	/**
	 * @see IMethodVariant#queryASTVariant(Value, Context)
	 */
	public final ValuedVariant<N> queryASTVariant(final Value v, final Context c) {
		return astvm.query(v, c);
	}

	/**
	 * @see IMethodVariant#queryParameterNode(int)
	 */
	public final N queryParameterNode(final int index) {
		N _temp = null;

		if (index >= 0 && index <= method.getParameterCount()) {
			_temp = parameters.get(index);
		}

		return _temp;
	}

	/**
	 * @see IMethodVariant#queryReturnNode()
	 */
	public final N queryReturnNode() {
		return returnVar;
	}

	/**
	 * @see IMethodVariant#queryThisNode()
	 */
	public final N queryThisNode() {
		return thisVar;
	}

	/**
	 * @see IMethodVariant#queryThrowNode(InvokeExpr, Context)
	 */
	public final N queryThrowNode(final InvokeExpr e, final Context c) {
		final InvocationVariant<N> _var = (InvocationVariant) queryASTVariant(e, c);
		N _temp = null;

		if (_var != null) {
			_temp = _var.getThrowNode();
		}
		return _temp;
	}

	/**
	 * Retrieves the node corresponding to the exceptions thrown by this method variant.
	 * 
	 * @return the node for thrown exceptions.
	 * @post result != null
	 */
	public final N queryThrownNode() {
		return thrownNode;
	}

	/**
	 * Decides if given type or references of it's type should be considered.
	 * 
	 * @param type of interest.
	 * @return <code>true</code> if it should be considered; <code>false</code>, otherwise.
	 */
	protected abstract boolean shouldConsider(Type type);
}

// End of File
