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

package edu.ksu.cis.indus.staticanalyses.concurrency.independence;

import edu.ksu.cis.indus.common.collections.CollectionUtils;
import edu.ksu.cis.indus.common.collections.IteratorUtils;
import edu.ksu.cis.indus.common.soot.SootPredicatesAndTransformers;

import edu.ksu.cis.indus.interfaces.IConcurrentIndependenceInfo;
import edu.ksu.cis.indus.interfaces.IEscapeInfo;

import edu.ksu.cis.indus.processing.AbstractProcessor;
import edu.ksu.cis.indus.processing.Context;
import edu.ksu.cis.indus.processing.ProcessingController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import soot.SootClass;
import soot.SootMethod;
import soot.ValueBox;

import soot.jimple.EnterMonitorStmt;
import soot.jimple.Stmt;

/**
 * This class provides independence detection that is based on escape analysis information. Atomicity is the property that
 * ensures the execution of a statement will only affect the state of the thread that executes it and not other threads.
 * <p>
 * By default, this class will treat methods of the following class as independent.
 * <ul>
 * <li> java.lang.String </li>
 * <li> java.lang.Integer </li>
 * <li> java.lang.Long </li>
 * <li> java.lang.Short </li>
 * <li> java.lang.Float </li>
 * <li> java.lang.Double </li>
 * <li> java.lang.Byte </li>
 * <li> java.lang.Boolean </li>
 * <li> java.lang.Character </li>
 * <li> java.lang.StringBuffer </li>
 * </ul>
 * </p>
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.6 $ $Date: 2007/02/10 19:07:29 $
 */
public class IndependentStmtDetector
		extends AbstractProcessor
		implements IConcurrentIndependenceInfo {

	/**
	 * The escape analysis to use.
	 */
	protected IEscapeInfo escapeInfo;

	/**
	 * The collection of independent statements.
	 *
	 * @invariant independentStmts.oclIsKindOf(Collection(Stmt))
	 */
	protected final Collection<Stmt> independentStmts = new ArrayList<Stmt>();

	/**
	 * A collection of names of independent classes.
	 */
	private final Collection<String> independentClassNames;

	/**
	 * This indicates if the currently being processed scope is independent.
	 */
	private boolean independentScope;

	/**
	 * Creates a new IndependentStmtDetector object.
	 */
	public IndependentStmtDetector() {
		independentClassNames = new ArrayList<String>();
		independentClassNames.add("java.lang.String");
		independentClassNames.add("java.lang.Integer");
		independentClassNames.add("java.lang.Float");
		independentClassNames.add("java.lang.Double");
		independentClassNames.add("java.lang.Boolean");
		independentClassNames.add("java.lang.Byte");
		independentClassNames.add("java.lang.Long");
		independentClassNames.add("java.lang.Short");
		independentClassNames.add("java.lang.Character");
		independentClassNames.add("java.lang.StringBuffer");
	}

	/**
	 * @see edu.ksu.cis.indus.processing.AbstractProcessor#callback(soot.SootClass)
	 */
	@Override public final void callback(final SootClass clazz) {
		super.callback(clazz);
		independentScope = independentClassNames.contains(clazz.getName());
	}

	/**
	 * @see edu.ksu.cis.indus.processing.IProcessor#callback(soot.jimple.Stmt, edu.ksu.cis.indus.processing.Context)
	 */
	@Override public final void callback(final Stmt stmt, final Context context) {
		if (independentScope
				|| (!stmt.containsArrayRef() && !stmt.containsFieldRef() && !stmt.containsInvokeExpr() && !(stmt instanceof EnterMonitorStmt))
				|| isIndependent(stmt, context.getCurrentMethod())) {
			independentStmts.add(stmt);
		}
	}

	/**
	 * @see edu.ksu.cis.indus.interfaces.IIdentification#getIds()
	 */
	public final Collection<? extends Comparable<?>> getIds() {
		return Collections.singleton(ID);
	}

	/**
	 * @see edu.ksu.cis.indus.processing.IProcessor#hookup(edu.ksu.cis.indus.processing.ProcessingController)
	 */
	public final void hookup(final ProcessingController ppc) {
		ppc.register(this);
		ppc.registerForAllStmts(this);
	}

	/**
	 * Checks if the statement is independent.
	 *
	 * @param stmt to be checked.
	 * @return <code>true</code> if <code>stmt</code> is independent; <code>false</code>, otherwise.
	 * @pre stmt != null
	 */
	public final boolean isIndependent(final Stmt stmt) {
		return independentStmts.contains(stmt);
	}

	/**
	 * @see edu.ksu.cis.indus.processing.IProcessor#reset()
	 */
	@Override public final void reset() {
		super.reset();
		independentStmts.clear();
	}

	/**
	 * Sets the names of the classes in which each method should be treated as independent.
	 *
	 * @param names is a collection of FQN of classes.
	 * @return the previous list of class names.
	 * @pre names != null
	 * @post result != null
	 */
	public Collection<String> setAtomicClassNames(final Collection<String> names) {
		final Collection<String> _result = new ArrayList<String>(independentClassNames);
		independentClassNames.clear();
		independentClassNames.addAll(names);
		return _result;
	}

	/**
	 * Sets the escape analysis to use.
	 *
	 * @param analysis to use.
	 * @pre analysis != null
	 */
	public final void setEscapeAnalysis(final IEscapeInfo analysis) {
		escapeInfo = analysis;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override public final String toString() {
		return CollectionUtils.prettyPrint(independentStmts);
	}

	/**
	 * @see edu.ksu.cis.indus.processing.IProcessor#unhook(edu.ksu.cis.indus.processing.ProcessingController)
	 */
	public final void unhook(final ProcessingController ppc) {
		ppc.unregister(this);
		ppc.unregisterForAllStmts(this);
	}

	/**
	 * Checks if the given statement is independent based on escape information.
	 *
	 * @param stmt to be tested.
	 * @param method in which the statement occurs.
	 * @return <code>true</code> if the statement is independent; <code>false</code>, otherwise.
	 * @pre stmt != null and method != null
	 */
	protected boolean isIndependent(final Stmt stmt, final SootMethod method) {
		boolean independent = !stmt.containsInvokeExpr();
		if (independent && stmt.containsFieldRef() && stmt.getFieldRef().getField().isStatic()) {
			independent = escapeInfo.escapes(stmt.getFieldRef().getField().getDeclaringClass(), method);
		}

		for (final Iterator<ValueBox> _i = IteratorUtils.filteredIterator(stmt.getUseAndDefBoxes().iterator(),
				SootPredicatesAndTransformers.ESCAPABLE_EXPR_FILTER); _i.hasNext() && independent;) {
			final ValueBox _vb = _i.next();
			independent = !escapeInfo.escapes(_vb.getValue(), method);
		}
		return independent;
	}
}

// End of File
