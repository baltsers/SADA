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

package edu.ksu.cis.indus.staticanalyses.callgraphs;

import edu.ksu.cis.indus.common.collections.MapUtils;
import edu.ksu.cis.indus.common.soot.Util;

import edu.ksu.cis.indus.interfaces.ICallGraphInfo.CallTriple;

import edu.ksu.cis.indus.processing.Context;
import edu.ksu.cis.indus.processing.ProcessingController;

import edu.ksu.cis.indus.staticanalyses.interfaces.IValueAnalyzer;
import edu.ksu.cis.indus.staticanalyses.processing.AbstractValueAnalyzerBasedProcessor;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.ValueBox;

import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.VirtualInvokeExpr;

/**
 * This implementation of <code>CallGraphInfo.ICallInfo</code> generates call info for a system based on the information
 * available from object flow information.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.14 $ $Date: 2007/07/24 10:06:07 $
 */
public class OFABasedCallInfoCollector
		extends AbstractValueAnalyzerBasedProcessor<Value>
		implements ICallInfoCollector {

	/**
	 * The logger used by instances of this class to log messages.
	 */
	static final Logger LOGGER = LoggerFactory.getLogger(OFABasedCallInfoCollector.class);

	/**
	 * The FA instance which implements object flow analysis. This instance is used to calculate call graphCache information.
	 * 
	 * @invariant analyzer.oclIsKindOf(OFAnalyzer)
	 */
	private IValueAnalyzer<Value> analyzer;

	/**
	 * This holds call information.
	 */
	private final CallInfo callInfoHolder = new CallInfo();

	/**
	 * {@inheritDoc}
	 */
	@Override public void callback(final SootMethod method) {
		// all method marked by the object flow analyses are reachable.
		callInfoHolder.addReachable(method);
	}

	/**
	 * Called by the post process controller when it walks a jimple value AST node.
	 * 
	 * @param vBox is the AST node to be processed.
	 * @param context in which value should be processed.
	 * @pre context != null and vBox != null
	 * @see edu.ksu.cis.indus.staticanalyses.interfaces.IValueAnalyzerBasedProcessor#callback(ValueBox,Context)
	 */
	@Override public void callback(final ValueBox vBox, final Context context) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("callback(ValueBox vBox = " + vBox + ", Context context = " + context + ") - BEGIN");
		}

		final Stmt _stmt = context.getStmt();
		final SootMethod _caller = context.getCurrentMethod();
		final Value _value = vBox.getValue();
		final InvokeExpr _invokeExpr = (InvokeExpr) _value;
		final SootMethod _callee = _invokeExpr.getMethod();

		if (_value instanceof StaticInvokeExpr || _value instanceof SpecialInvokeExpr) {
			final Collection<CallTriple> _callees = MapUtils.getCollectionFromMap(callInfoHolder.caller2callees, _caller);
			final CallTriple _triple1 = new CallTriple(_callee, _stmt, _invokeExpr);
			_callees.add(_triple1);

			final Collection<CallTriple> _callers = MapUtils.getCollectionFromMap(callInfoHolder.callee2callers, _callee);
			final CallTriple _triple2 = new CallTriple(_caller, _stmt, _invokeExpr);
			_callers.add(_triple2);
		} else {
			callBackOnInstanceInvokeExpr(context, (InstanceInvokeExpr) _value);
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("callback() - END");
		}
	}

	/**
	 * This calculates information such as heads, tails, and such.
	 * 
	 * {@inheritDoc}
	 */
	@Override public void consolidate() {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("BEGIN: call graph consolidation");
		}

		callInfoHolder.fixupMethodsHavingZeroCallersAndCallees();

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Call Graph Info - " + callInfoHolder.toString());
		}
		
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("END: call graph consolidation");
		}
		
		stable();
	}

	/**
	 * {@inheritDoc}
	 */
	public CallGraphInfo.ICallInfo getCallInfo() {
		return callInfoHolder;
	}

	/**
	 * {@inheritDoc}
	 */
	public void hookup(final ProcessingController ppc) {
		unstable();
		ppc.register(VirtualInvokeExpr.class, this);
		ppc.register(InterfaceInvokeExpr.class, this);
		ppc.register(StaticInvokeExpr.class, this);
		ppc.register(SpecialInvokeExpr.class, this);
		ppc.register(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override public void processingBegins() {
		unstable();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override public void reset() {
		callInfoHolder.reset();
	}

	/**
	 * Sets the analyzer to be used to calculate call graph information upon call back.
	 * 
	 * @param objFlowAnalyzer that provides the information to create the call graph.
	 * @pre objFlowAnalyzer != null and objFlowAnalyzer.oclIsKindOf(OFAnalyzer)
	 * @see edu.ksu.cis.indus.staticanalyses.interfaces.IValueAnalyzerBasedProcessor#setAnalyzer(IValueAnalyzer)
	 */
	@Override public void setAnalyzer(final IValueAnalyzer<Value> objFlowAnalyzer) {
		analyzer = objFlowAnalyzer;
	}

	/**
	 * {@inheritDoc}
	 */
	public void unhook(final ProcessingController ppc) {
		ppc.unregister(VirtualInvokeExpr.class, this);
		ppc.unregister(InterfaceInvokeExpr.class, this);
		ppc.unregister(StaticInvokeExpr.class, this);
		ppc.unregister(SpecialInvokeExpr.class, this);
		ppc.unregister(this);
		stable();
	}

	/**
	 * Called as a result of callback durign processing the AST when instance invoke expression is encountered.
	 * 
	 * @param context in which expression should be processed.
	 * @param expr is the expression.
	 * @pre context != null and stmt != null and caller != null and expr != null
	 */
	private void callBackOnInstanceInvokeExpr(final Context context, final InstanceInvokeExpr expr) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("callBackOnInstanceInvokeExpr(Context context = " + context + ", InstanceInvokeExpr expr = " + expr
					+ ") - BEGIN");
		}

		final Stmt _stmt = context.getStmt();
		final SootMethod _caller = context.getCurrentMethod();
		final SootMethod _calleeMethod = expr.getMethod();
		context.setProgramPoint(expr.getBaseBox());

		final Collection<?> _values = analyzer.getValues(expr.getBase(), context);
		final Map<SootMethod, Collection<CallTriple>> _callee2callers = callInfoHolder.callee2callers;
		final Map<SootMethod, Collection<CallTriple>> _caller2callees = callInfoHolder.caller2callees;
		final Collection<CallTriple> _callees = MapUtils.getCollectionFromMap(_caller2callees, _caller);
		final CallTriple _ctrp = new CallTriple(_caller, _stmt, expr);
		
		if (!_values.isEmpty()) {
			for (final Iterator<?> _i = _values.iterator(); _i.hasNext();) {
				final Object _t = _i.next();
				SootClass _accessClass = null;

				if (_t instanceof NewExpr) {
					final NewExpr _newExpr = (NewExpr) _t;
					_accessClass = analyzer.getEnvironment().getClass(_newExpr.getBaseType().getClassName());
				} else if (_t instanceof StringConstant) {
					_accessClass = analyzer.getEnvironment().getClass("java.lang.String");
				} else if (_t instanceof NewArrayExpr || _t instanceof NewMultiArrayExpr) {
					_accessClass = analyzer.getEnvironment().getClass("java.lang.Object");
				} else {
					continue;
				}

				final String _methodName = _calleeMethod.getName();
				final List<Type> _parameterTypes = _calleeMethod.getParameterTypes();
				final Type _returnType = _calleeMethod.getReturnType();
				final SootMethod _callee = Util.findMethodImplementation(_accessClass, _methodName, _parameterTypes,
						_returnType);
				final CallTriple _triple = new CallTriple(_callee, _stmt, expr);
				_callees.add(_triple);

				final Collection<CallTriple> _callers = MapUtils.getCollectionFromMap(_callee2callers, _callee);
				_callers.add(_ctrp);
			}
		} else if (_calleeMethod.isConcrete()){
			final CallTriple _triple = new CallTriple(_calleeMethod, _stmt, expr);
			_callees.add(_triple);

			final Collection<CallTriple> _callers = MapUtils.getCollectionFromMap(_callee2callers, _calleeMethod);
			_callers.add(_ctrp);
      callInfoHolder.addReachable(_calleeMethod);
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("callBackOnInstanceInvokeExpr() - END");
		}
	}
}

// End of File
