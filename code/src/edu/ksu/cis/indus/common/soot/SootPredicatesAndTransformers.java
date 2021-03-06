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

import edu.ksu.cis.indus.annotations.Empty;
import edu.ksu.cis.indus.common.collections.IPredicate;
import edu.ksu.cis.indus.common.collections.InstanceOfPredicate;

import soot.Local;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.ArrayRef;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.NewExpr;
import soot.jimple.NullConstant;
import soot.jimple.ParameterRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.ThisRef;

/**
 * This class contains <i>jakarta commons collections</i> related predicates and transformers that are specific to Soot AST
 * types.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.9 $ $Date: 2007/10/09 15:59:12 $
 */
public final class SootPredicatesAndTransformers {

	/**
	 * A predicate used to filter <code>EnterMonitorStmt</code>.
	 */
	public static final IPredicate<Stmt> ENTER_MONITOR_STMT_PREDICATE = new InstanceOfPredicate<EnterMonitorStmt, Stmt>(
			EnterMonitorStmt.class);

	/**
	 * This filter is used to identify AST chunks that may represent references that can escape.
	 */
	public static final IPredicate<ValueBox> ESCAPABLE_EXPR_FILTER = new IPredicate<ValueBox>() {

		public boolean evaluate(final ValueBox object) {
			final Value _v = object.getValue();
			return _v instanceof StaticFieldRef || _v instanceof InstanceFieldRef || _v instanceof ArrayRef
					|| _v instanceof Local || _v instanceof ThisRef || _v instanceof ParameterRef;
		}
	};

	/**
	 * A predicate used to filter statements with invoke expressions. Filter expression is
	 * <code>((Stmt)o).containsInvokeExpr()</code>.
	 */
	public static final IPredicate<Stmt> INVOKING_STMT_PREDICATE = new IPredicate<Stmt>() {

		public  boolean evaluate(final Stmt object) {
			return object.containsInvokeExpr();
		}
	};
	public static final IPredicate<Unit> INVOKING_UNIT_PREDICATE = new IPredicate<Unit>() {

		public  boolean evaluate(Stmt object) {
			return object.containsInvokeExpr();
		}

		@Override
		public boolean evaluate(Unit object) {
			// TODO Auto-generated method stub
			Stmt tmpStmt=(Stmt) object;
			return tmpStmt.containsInvokeExpr();
		}
	};
	/**
	 * A predicate used to filter <code>EnterMonitorStmt</code>.
	 */
	public static final IPredicate<Value> NEW_EXPR_PREDICATE = new InstanceOfPredicate<NewExpr, Value>(NewExpr.class);

	/**
	 * This predicate filters out <code>NullConstant</code> values.
	 */
	public static final IPredicate<Value> NULL_PREDICATE = new InstanceOfPredicate<NullConstant, Value>(NullConstant.class);

	/**
	 * Creates an instance of this class.
	 */
	@Empty public SootPredicatesAndTransformers() {
		super();
	}
}

// End of File
