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

import edu.ksu.cis.indus.annotations.Functional;
import edu.ksu.cis.indus.annotations.Immutable;
import edu.ksu.cis.indus.annotations.NonNull;
import edu.ksu.cis.indus.common.scoping.SpecificationBasedScopeDefinition;
import edu.ksu.cis.indus.interfaces.IEnvironment;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Local;
import soot.RefType;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.VoidType;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.Stmt;
import soot.toolkits.graph.UnitGraph;

/**
 * This class provides the an abstract implementation of <code>IStmtGraphFactory</code> via which unit graphs can be
 * retrieved. The subclasses should provide suitable unit graph implementation. The control flow edges in the provided unit
 * graphs are pruned by matching the thrown exceptions to the enclosing catch blocks. Refer to
 * <code>Util.pruneExceptionBasedControlFlow()</code> for more information.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.21 $ $Date: 2007/02/10 19:08:36 $
 * @param <T> the type of the manufactored graph.
 */
public abstract class AbstractStmtGraphFactory<T extends UnitGraph>
		implements IStmtGraphFactory<T> {

	/**
	 * The logger used by instances of this class to log messages.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStmtGraphFactory.class);

	/**
	 * The scope specification.
	 */
	protected SpecificationBasedScopeDefinition scope;

	/**
	 * The environment with which this factory is associated with.
	 */
	@NonNull private IEnvironment environment;

	/**
	 * This maps methods to unit graphs.
	 */
	private final Map<SootMethod, T> method2UnitGraph = new HashMap<SootMethod, T>(Constants.getNumOfMethodsInApplication());

	/**
	 * Retrieves the unit graph of the given method.
	 * 
	 * @param method for which the unit graph is requested.
	 * @return the requested unit graph.
	 * @post method.isConcrete() implies result.getBody() = method.getBody()
	 * @post (not method.isConcrete()) implies result.getBody() != method.getBody()
	 */
	@NonNull public final T getStmtGraph(@NonNull @Immutable final SootMethod method) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getStmtGraph(method = " + method + ")");
		}

		T _result = method2UnitGraph.get(method);

		if (_result == null) {
			_result = getBodyForMethod(method);

			if (_result == null) {
				// stub in an empty graph.
				final Jimple _jimple = Jimple.v();
				final JimpleBody _body = _jimple.newBody();
				@SuppressWarnings("unchecked") final Collection<Unit> _units = _body.getUnits();
				_body.setMethod(method);

				if (!method.isStatic()) {
					final RefType _thisType = method.getDeclaringClass().getType();
					final Local _newLocal = _jimple.newLocal("r0", _thisType);
					_body.getLocals().add(_newLocal);
					_units.add(_jimple.newIdentityStmt(_newLocal, _jimple.newThisRef(_thisType)));
				}

				if (method.getParameterCount() > 0) {
					int _j = 0;
					for (@SuppressWarnings("unchecked") final Iterator<Type> _i = method.getParameterTypes().iterator(); _i
							.hasNext();) {
						final Type _type = _i.next();
						final Local _newLocal = _jimple.newLocal("p" + _j, _type);
						_body.getLocals().add(_newLocal);
						_units.add(_jimple.newIdentityStmt(_newLocal, _jimple.newParameterRef(_type,
								_j++)));
					}
				}

				if (method.getReturnType() instanceof VoidType) {
					_units.add(_jimple.newReturnVoidStmt());
				} else {
					_units.add(_jimple.newReturnStmt(Util.getDefaultValueFor(method.getReturnType())));
				}
				_result = getStmtGraphForBody(_body);
			}
			method2UnitGraph.put(method, _result);
		}
		return _result;
	}

	/**
	 * Resets all internal datastructures.
	 */
	public final void reset() {
		method2UnitGraph.clear();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setScope(final SpecificationBasedScopeDefinition scopeDef, final IEnvironment env) {
		scope = scopeDef;
		environment = env;
	}

	/**
	 * Retrieves the body for the given method.
	 * 
	 * @param method of interest.
	 * @return the method body.
	 */
	protected T getBodyForMethod(@NonNull final SootMethod method) {
		T _result = null;
		if (isInScope(method)) {
			final JimpleBody _body = (JimpleBody) method.retrieveActiveBody();
			_result = getStmtGraphForBody(_body);
		} else if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Method " + method + " is out of scope or is not concrete.");
		}
		return _result;
	}

	/**
	 * Retreives the unit graph (of a particular implementation) for the given body.
	 * 
	 * @param body to be represented as a graph.
	 * @return a unit graph.
	 */
	@NonNull protected abstract T getStmtGraphForBody(@NonNull final JimpleBody body);

	/**
	 * Checks if the given method is in the scope.
	 * 
	 * @param method of interest.
	 * @return <code>true</code> if the method is in scope; <code>false</code>, otherwise.
	 */
	@Functional protected final boolean isInScope(@NonNull final SootMethod method) {
		return (scope == null || scope.isInScope(method, environment)) && method.isConcrete();
	}
}

// End of File
