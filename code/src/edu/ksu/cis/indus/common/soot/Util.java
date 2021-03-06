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
import edu.ksu.cis.indus.annotations.Functional;
import edu.ksu.cis.indus.annotations.Immutable;
import edu.ksu.cis.indus.annotations.NonNull;
import edu.ksu.cis.indus.annotations.NonNullContainer;
import edu.ksu.cis.indus.common.datastructures.HistoryAwareFIFOWorkBag;
import edu.ksu.cis.indus.common.datastructures.HistoryAwareLIFOWorkBag;
import edu.ksu.cis.indus.common.datastructures.IWorkBag;
import edu.ksu.cis.indus.interfaces.ICallGraphInfo;
import edu.ksu.cis.indus.interfaces.IEnvironment;
import edu.ksu.cis.indus.processing.Context;
import edu.ksu.cis.indus.processing.OneAllStmtSequenceRetriever;
import edu.ksu.cis.indus.processing.ProcessingController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.ArrayType;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.Local;
import soot.LongType;
import soot.Modifier;
import soot.NullType;
import soot.PatchingChain;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.ShortType;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.ClassMember;
import soot.Trap;
import soot.Type;
import soot.Value;
import soot.VoidType;
import soot.jimple.DoubleConstant;
import soot.jimple.FloatConstant;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.LongConstant;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.tagkit.Host;

/**
 * General utility class providing common chore methods.
 * 
 * @author <a href="mailto:rvprasad@cis.ksu.edu">Venkatesh Prasad Ranganath</a>
 * @version $Revision: 1.54 $ $Date: 2007/02/10 19:08:36 $
 */
public final class Util {

	/**
	 * The logger used by instances of this class to log messages.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

	// /CLOVER:OFF

	/**
	 * A private constructor to prevent the instantiation of this class.
	 */
	@Empty private Util() {
		super();
	}

	// /CLOVER:ON

	/**
	 * Erases given classes in the given environment while trying to keep the environment type safe.
	 * 
	 * @param classes to be erased.
	 * @param env to be updated.
	 * @return the classes that were erased. It is possible that for safety reasons some classes are retained.
	 */
	@NonNullContainer @NonNull @Immutable public static Collection<SootClass> eraseClassesFrom(
			@Immutable @NonNull @NonNullContainer final Collection<SootClass> classes, @NonNull final IEnvironment env) {
		final Collection<SootClass> _result = new ArrayList<SootClass>(classes);
		final ProcessingController _pc = new ProcessingController();
		final OneAllStmtSequenceRetriever _ssr = new OneAllStmtSequenceRetriever();
		_ssr.setStmtGraphFactory(new CompleteStmtGraphFactory());
		_pc.setStmtSequencesRetriever(_ssr);
		_pc.setEnvironment(env);

		final ClassEraser _ece = new ClassEraser(_result);
		_ece.hookup(_pc);
		_pc.process();
		_ece.unhook(_pc);
		_pc.reset();

		@SuppressWarnings("unchecked") final Iterator<SootClass> _j = classes.iterator();
		final int _jEnd = classes.size();

		for (int _jIndex = 0; _jIndex < _jEnd; _jIndex++) {
			final SootClass _o = _j.next();

			if (_result.contains(_o)) {
				env.removeClass(_o);
			}
		}
		return _result;
	}

	/**
	 * Returns the class, starting from the given class and above it in the class hierarchy, that declares the given method.
	 * 
	 * @param sc the class from which to start the search in the class hierarchy. This parameter cannot be <code>null</code>.
	 * @param sm the method to search for in the class hierarchy. This parameter cannot be <code>null</code>.
	 * @return the <code>SootMethod</code> corresponding to the implementation of <code>sm</code>.
	 * @throws IllegalStateException if <code>sm</code> is not available in the given branch of the class hierarchy.
	 */
	@Functional @NonNull public static SootMethod findDeclaringMethod(@NonNull final SootClass sc,
			@NonNull final SootMethod sm) {
		final SootMethod _result;

		if (sc.declaresMethod(sm.getName(), sm.getParameterTypes(), sm.getReturnType())) {
			_result = sc.getMethod(sm.getName(), sm.getParameterTypes(), sm.getReturnType());
		} else if (hasSuperclass(sc)) {
			_result = findDeclaringMethod(sc.getSuperclass(), sm);
		} else {
			throw new IllegalStateException("Method " + sm + " not available in class " + sc + ".");
		}
		return _result;
	}

	/**
	 * Finds the implementation of the given method defined in <code>accessClass</code> or its superclasses.
	 * 
	 * @param accessClass is the class via which the method is invoked.
	 * @param methodName is the name of the method.
	 * @param parameterTypes is the list of parameter types of the method.
	 * @param returnType is the return type of the method.
	 * @return the implementation of the requested method if present in the class hierarchy; <code>null</code>, otherwise.
	 * @pre accessClass != null and methodName != null and parameterTypes != null and returnType != null
	 */
	@Functional public static SootMethod findMethodImplementation(@NonNull final SootClass accessClass,
			@Immutable @NonNull final String methodName, @NonNull @NonNullContainer final List<Type> parameterTypes,
			@NonNull final Type returnType) {
		SootMethod _result = null;

		if (accessClass.declaresMethod(methodName, parameterTypes, returnType)) {
			_result = accessClass.getMethod(methodName, parameterTypes, returnType);
		} else {
			if (accessClass.hasSuperclass()) {
				final SootClass _superClass = accessClass.getSuperclass();
				_result = findMethodImplementation(_superClass, methodName, parameterTypes, returnType);
			} else {
				if (LOGGER.isErrorEnabled()) {
					LOGGER.error(methodName + "(" + parameterTypes + "):" + returnType + " is not accessible from "
							+ accessClass);
				}
			}
		}
		return _result;
	}

	/**
	 * Finds the method declarations that match the given method in its declaring class and the super interface/classes of the
	 * declaring class.
	 * 
	 * @param method of interest.
	 * @return a collection of methods.
	 */
	@Functional @NonNullContainer @NonNull public static Collection<SootMethod> findMethodInSuperClassesAndInterfaces(
			@NonNull final SootMethod method) {
		final IWorkBag<SootClass> _toProcess = new HistoryAwareFIFOWorkBag<SootClass>(new HashSet<SootClass>());
		Collection<SootMethod> _result = new HashSet<SootMethod>();
		_toProcess.addWork(method.getDeclaringClass());

		@SuppressWarnings("unchecked") final List<Type> _parameterTypes = method.getParameterTypes();
		final Type _retType = method.getReturnType();
		final String _methodName = method.getName();

		while (_toProcess.hasWork()) {
			final SootClass _sc = _toProcess.getWork();

			if (_sc.declaresMethod(_methodName, _parameterTypes, _retType)) {
				_result.add(_sc.getMethod(_methodName, _parameterTypes, _retType));
			}

			if (hasSuperclass(_sc)) {
				final SootClass _superClass = _sc.getSuperclass();

				_toProcess.addWorkNoDuplicates(_superClass);
			}

			for (@SuppressWarnings("unchecked") final Iterator<SootClass> _i = _sc.getInterfaces().iterator(); _i.hasNext();) {
				final SootClass _interface = _i.next();

				_toProcess.addWorkNoDuplicates(_interface);
			}
		}

		if (_result.isEmpty()) {
			_result = Collections.emptyList();
		}
		return _result;
	}

	/**
	 * Fixes the body of <code>java.lang.Thread.start()</code> (only if it is native) to call <code>run()</code> on the
	 * target or self. This is required to complete the call graph. This leaves the body untouched if it is not native.
	 * 
	 * @param scm is the scene in which the alteration occurs.
	 */
	@SuppressWarnings("unchecked") @Immutable public static void fixupThreadStartBody(@NonNull final Scene scm) {
		final SootClass _declClass = scm.getSootClass("java.lang.Thread");

		if (_declClass != null) {
			final SootMethod _sm = _declClass.getMethodByName("start");

			if (_sm != null) {
				JimpleBody _threadStartBody = null;

				if (_sm.isConcrete()) {
					_threadStartBody = (JimpleBody) _sm.retrieveActiveBody();
				}

				if (_threadStartBody == null) {
					_declClass.setApplicationClass();
					((ClassMember) _declClass).setPhantom(false);
					//_declClass.setPhantom(false);
					_sm.setModifiers(_sm.getModifiers() & ~Modifier.NATIVE);
					_sm.setPhantom(false);

					final Jimple _jimple = Jimple.v();
					_threadStartBody = _jimple.newBody(_sm);
					final PatchingChain _sl = _threadStartBody.getUnits();
					final Local _thisRef = _jimple.newLocal("$this", RefType.v(_declClass.getName()));
					_threadStartBody.getLocals().add(_thisRef);
					// adds $this := @this;
					_sl.addFirst(_jimple.newIdentityStmt(_thisRef, _jimple.newThisRef(RefType.v(_declClass))));

					// adds $this.virtualinvoke[java.lang.Thread.run()]:void;
					final VirtualInvokeExpr _ve = _jimple.newVirtualInvokeExpr(_thisRef, (SootMethodRef) _declClass.getMethodByName("run"),
							Collections.EMPTY_LIST);
					_sl.addLast(_jimple.newInvokeStmt(_ve));
					_sl.addLast(_jimple.newReturnVoidStmt());
					_sm.setActiveBody(_threadStartBody);

					if (LOGGER.isWarnEnabled()) {
						LOGGER.warn("Fixed up java.lang.Thread.start() body");
					}
				}
			}
		}
	}

	/**
	 * Retrieves all ancestors (classes/interfaces) of the given class.
	 * 
	 * @param sootClass for which the ancestors are requested.
	 * @return a collection of classes.
	 */
	@Functional @NonNullContainer @NonNull public static Collection<SootClass> getAncestors(@NonNull final SootClass sootClass) {
		final Collection<SootClass> _result = new HashSet<SootClass>();
		final Collection<SootClass> _temp = new HashSet<SootClass>();
		final IWorkBag<SootClass> _wb = new HistoryAwareLIFOWorkBag<SootClass>(_result);
		_wb.addWork(sootClass);

		while (_wb.hasWork()) {
			final SootClass _work = _wb.getWork();

			if (hasSuperclass(_work)) {
				final SootClass _superClass = _work.getSuperclass();
				_temp.add(_superClass);
			}
			_temp.addAll(_work.getInterfaces());

			for (final Iterator<SootClass> _i = _temp.iterator(); _i.hasNext();) {
				final SootClass _sc = _i.next();

				_wb.addWorkNoDuplicates(_sc);
			}
		}
		_result.remove(sootClass);
		return _result;
	}

	/**
	 * Provides the class which injects the given method into the specific branch of the inheritence hierarchy which contains
	 * the given class.
	 * 
	 * @param sc is the class in or above which the method may be defined.
	 * @param method is the name of the method (not the fully classified name).
	 * @param parameterTypes list of type of the parameters of the method.
	 * @param returnType return type of the method.
	 * @return if such a method exists, the class that injects the method is returned. <code>null</code> is returned,
	 *         otherwise.
	 */
	@Functional public static SootClass getDeclaringClass(@NonNull final SootClass sc, @NonNull final String method,
			@NonNullContainer @NonNull final List<Type> parameterTypes, @NonNull final Type returnType) {
		SootClass _contains = sc;

		while (!_contains.declaresMethod(method, parameterTypes, returnType)) {
			if (hasSuperclass(_contains)) {
				_contains = _contains.getSuperclass();
			} else {
				_contains = null;
				break;
			}
		}

		return _contains;
	}

	/**
	 * Retrieves the default value for the given type.
	 * 
	 * @param type for which the default value is requested.
	 * @return the default value
	 * @throws IllegalArgumentException when an invalid type is provided.
	 */
	@NonNull @Functional public static Value getDefaultValueFor(@NonNull final Type type) {
		Value _result = null;

		if (type instanceof RefLikeType) {
			_result = NullConstant.v();
		} else if (type instanceof IntType) {
			_result = IntConstant.v(0);
		} else if (type instanceof CharType) {
			_result = IntConstant.v(0);
		} else if (type instanceof ByteType) {
			_result = IntConstant.v(0);
		} else if (type instanceof BooleanType) {
			_result = IntConstant.v(0);
		} else if (type instanceof DoubleType) {
			_result = DoubleConstant.v(0);
		} else if (type instanceof FloatType) {
			_result = FloatConstant.v(0);
		} else if (type instanceof LongType) {
			_result = LongConstant.v(0);
		} else if (type instanceof ShortType) {
			_result = IntConstant.v(0);
		} else {
			final String _msg = "Illegal type specified.";
			LOGGER.error(_msg + type);
			throw new IllegalArgumentException(_msg + type);
		}

		return _result;
	}

	/**
	 * Retrieves the maximal subset of traps from <code>traps</code> such that each trap encloses the <code>stmt</code> in
	 * the list of statements <code>stmtList</code>.
	 * 
	 * @param traps is the list of traps.
	 * @param stmt is the statement being enclosed.
	 * @param stmtList is the list of statements.
	 * @return a list of traps.
	 * @pre stmtList.contains(stmt)
	 * @post traps.containsAll(result)
	 */
	@Functional @NonNullContainer @NonNull public static List<Trap> getEnclosingTrap(
			@NonNull @NonNullContainer final List<Trap> traps, @NonNull final Stmt stmt,
			@NonNull @NonNullContainer final List<Stmt> stmtList) {
		final List<Trap> _result = new ArrayList<Trap>();
		final int _stmtIndex = stmtList.indexOf(stmt);
		for (final Trap _trap : traps) {
			if (stmtList.indexOf(_trap.getBeginUnit()) <= _stmtIndex && _stmtIndex <= stmtList.indexOf(_trap.getEndUnit())) {
				_result.add(_trap);
			}
		}
		return _result;
	}

	/**
	 * Retrieves the hosts which are tagged with a tag named <code>tagName</code>.
	 * 
	 * @param <T> the type of the host.
	 * @param hosts is the collection of hosts to filter.
	 * @param tagName is the name of the tag to filter <code>hosts</code>.
	 * @return a collection of hosts.
	 * @post result->forall(o | hosts->contains(o) and o.hasTag(tagName))
	 */
	@Functional @NonNull public static <T extends Host> Collection<T> getHostsWithTag(
			@NonNullContainer @NonNull final Collection<T> hosts, @NonNull final String tagName) {
		Collection<T> _result = new ArrayList<T>();

		for (final Iterator<T> _i = hosts.iterator(); _i.hasNext();) {
			final T _host = _i.next();

			if (_host.hasTag(tagName)) {
				_result.add(_host);
			}
		}

		if (_result.isEmpty()) {
			_result = Collections.emptyList();
		}
		return _result;
	}

	/**
	 * Retrieves the type object for the given primitive (non-array) type in the given scene.
	 * 
	 * @param typeName is the name of the type.
	 * @param scene contains the classes that make up the system.
	 * @return the type object
	 * @throws MissingResourceException when the named type does not exists.
	 * @pre typeName.indexOf("[") = -1
	 */
	@Immutable public static Type getPrimitiveTypeFor(@NonNull @Immutable final String typeName, @NonNull final Scene scene) {
		final Type _result;

		if (typeName.equals("int")) {
			_result = IntType.v();
		} else if (typeName.equals("char")) {
			_result = CharType.v();
		} else if (typeName.equals("byte")) {
			_result = ByteType.v();
		} else if (typeName.equals("long")) {
			_result = LongType.v();
		} else if (typeName.equals("short")) {
			_result = ShortType.v();
		} else if (typeName.equals("float")) {
			_result = FloatType.v();
		} else if (typeName.equals("double")) {
			_result = DoubleType.v();
		} else if (typeName.equals("boolean")) {
			_result = BooleanType.v();
		} else if (typeName.equals("void")) {
			_result = VoidType.v();
		} else {
			final SootClass _sc = scene.getSootClass(typeName);

			if (_sc == null) {
				final String _msg = typeName + " is not available in the System.";
				LOGGER.error(_msg);
				throw new MissingResourceException("Given type is not available in the System", typeName, null);
			}
			_result = _sc.getType();
		}
		return _result;
	}

	/**
	 * Retrieves the methods that can be invoked externally (this rules out execution of super methods) on instances of the
	 * given classes.
	 * 
	 * @param newClasses is a collection of classes to be processed.
	 * @return the resolved methods
	 */
	@NonNull @Functional public static Collection<SootMethod> getResolvedMethods(
			@NonNullContainer @NonNull final Collection<SootClass> newClasses) {
		final Collection<SootMethod> _col = new HashSet<SootMethod>();
		final Collection<String> _temp = new HashSet<String>();
		final Iterator<SootClass> _i = newClasses.iterator();
		final int _iEnd = newClasses.size();

		for (int _iIndex = 0; _iIndex < _iEnd; _iIndex++) {
			final SootClass _sc = _i.next();
			@SuppressWarnings("unchecked") final Collection<SootMethod> _methods = _sc.getMethods();
			_col.addAll(_methods);
			_temp.clear();

			final Iterator<SootMethod> _l = _methods.iterator();
			final int _lEnd = _methods.size();

			for (int _lIndex = 0; _lIndex < _lEnd; _lIndex++) {
				final SootMethod _sm = _l.next();
				_temp.add(_sm.getSubSignature());
			}

			SootClass _super = _sc;

			while (hasSuperclass(_super)) {
				_super = _super.getSuperclass();

				@SuppressWarnings("unchecked") final Iterator<SootMethod> _j = _super.getMethods().iterator();
				final int _jEnd = _super.getMethods().size();

				for (int _jIndex = 0; _jIndex < _jEnd; _jIndex++) {
					final SootMethod _superMethod = _j.next();

					if (!_temp.contains(_superMethod.getSubSignature())) {
						_temp.add(_superMethod.getSubSignature());
						_col.add(_superMethod);
					}
				}
			}
		}

		final Iterator<SootMethod> _j = _col.iterator();
		final int _jEnd = _col.size();

		for (int _jIndex = 0; _jIndex < _jEnd; _jIndex++) {
			final SootMethod _sm = _j.next();

			if (_sm.getName().equals("<init>") && !newClasses.contains(_sm.getDeclaringClass())) {
				_j.remove();
			}
		}

		return _col;
	}

	/**
	 * Retrieve the soot options to be used when using Indus modules. These options should be used via
	 * <code>Options.v().parse(getSootOptions())</code>. These options are setup according to the requirement of the
	 * analyses in the project.
	 * 
	 * @return the soot options.
	 */
	@NonNull @NonNullContainer @Functional public static String[] getSootOptions() {
		final String[] _options = { "-p", "jb", "enabled:true,use-original-names:false", "-p", "jb.ls", "enabled:true", "-p",
				"jb.a", "enabled:true,only-stack-locals:true", "-p", "jb.ulp", "enabled:false", };

		return _options;
	}

	/**
	 * Retrieves the type object for the given type in the given scene.
	 * 
	 * @param typeName is the name of the type.
	 * @param scene contains the classes that make up the system.
	 * @return the type object.
	 */
	@Immutable @NonNull public static Type getTypeFor(@NonNull @Immutable final String typeName, @NonNull final Scene scene) {
		final Type _result;
		final int _i = typeName.indexOf("[", 0);

		if (_i == -1) {
			_result = getPrimitiveTypeFor(typeName, scene);
		} else {
			_result = ArrayType.v(getPrimitiveTypeFor(typeName.substring(0, _i), scene), typeName.length() - _i);
		}
		return _result;
	}

	/**
	 * Checks if the given class has a super class. <code>java.lang.Object</code> will not have a super class, but others
	 * will.
	 * 
	 * @param sc is the class to be tested.
	 * @return <code>true</code> if <code>sc</code> has a superclass; <code>false</code>, otherwise.
	 * @post sc.getName().equals("java.lang.Object") implies result = false
	 * @post (not sc.getName().equals("java.lang.Object")) implies result = true
	 */
	@Functional public static boolean hasSuperclass(@NonNull final SootClass sc) {
		boolean _result = false;

		if (!sc.getName().equals("java.lang.Object")) {
			_result = sc.hasSuperclass();
		}
		return _result;
	}

	/**
	 * Checks if the given class implements the named interface.
	 * 
	 * @param child is the class to be tested for implementation.
	 * @param ancestor is the fully qualified name of the interface to be checked for implementation.
	 * @return <code>true</code> if <code>child</code> implements the named interface; <code>false</code>, otherwise.
	 * @post result == (child.evaluationType().allSuperTypes()->forall(o | o.name() = ancestor))
	 */
	@Functional public static boolean implementsInterface(@NonNull final SootClass child, @NonNull final String ancestor) {
		boolean _result = false;
		SootClass _temp = child;

		while (!_result && (_temp.getInterfaceCount() > 0 || hasSuperclass(_temp))) {
			if (_temp.implementsInterface(ancestor)) {
				_result = true;
			} else {
				_temp = _temp.getSuperclass();
			}
		}
		return _result;
	}

	/**
	 * Checks if one class is the descendent of another. It is assumed that a class cannot be it's own ancestor.
	 * 
	 * @param child class whose ancestor is of interest.
	 * @param ancestor the ancestor class.
	 * @return <code>true</code> if <code>ancestor</code> class is indeed the ancestor of <code>child</code> class;
	 *         false otherwise.
	 * @post result == child.oclIsKindOf(ancestor)
	 */
	@Functional public static boolean isDescendentOf(@NonNull final SootClass child, @NonNull final SootClass ancestor) {
		return isDescendentOf(child, ancestor.getName());
	}

	/**
	 * Checks if one class is the descendent of another. It is assumed that a class cannot be it's own ancestor.
	 * 
	 * @param child class whose ancestor is of interest.
	 * @param ancestor fully qualified name of the ancestor class.
	 * @return <code>true</code> if a class by the name of <code>ancestor</code> is indeed the ancestor of
	 *         <code>child</code> class; false otherwise.
	 * @post result == (child.evaluationType().allSuperTypes()->forall(o | o.name() = ancestor))
	 */
	@Functional public static boolean isDescendentOf(@NonNull final SootClass child, @NonNull final String ancestor) {
		boolean _retval = false;
		SootClass _temp = child;

		while (!_retval) {
			if (_temp.getName().equals(ancestor)) {
				_retval = true;
			} else {
				if (hasSuperclass(_temp)) {
					_temp = _temp.getSuperclass();
				} else {
					break;
				}
			}
		}

		if (!_retval) {
			_retval = implementsInterface(child, ancestor);
		}

		return _retval;
	}

	/**
	 * Checks if the given classes are on the same class hierarchy branch. This means either one of the classes should be the
	 * subclass of the other class.
	 * 
	 * @param class1 one of the two classes to be checked for relation.
	 * @param class2 one of the two classes to be checked for relation.
	 * @return <code>true</code> if <code>class1</code> is reachable from <code>class2</code>; <code>false</code>,
	 *         otherwise.
	 * @post result == (class1.oclIsKindOf(class2) or class2.oclIsKindOf(class1))
	 */
	@Functional public static boolean isHierarchicallyRelated(@NonNull final SootClass class1, @NonNull final SootClass class2) {
		return class1.equals(class2) || isDescendentOf(class1, class2.getName()) || isDescendentOf(class2, class1.getName());
	}

	/**
	 * Checks if the method invoked at the invocation site is one of the <code>notify</code> methods in
	 * <code>java.lang.Object</code> class based on the given call graph.
	 * 
	 * @param stmt in which the invocation occurs.
	 * @param method in which <code>stmt</code> occurs.
	 * @param cgi to be used in the check.
	 * @return <code>true</code> if the method invoked at the invocation site is one of the <code>notify</code> methods in
	 *         <code>java.lang.Object</code> class; <code>false</code>, otherwise.
	 */
	@Functional public static boolean isNotifyInvocation(@NonNull final InvokeStmt stmt, final SootMethod method,
			final ICallGraphInfo cgi) {
		final InvokeExpr _expr = stmt.getInvokeExpr();
		final SootMethod _sm = _expr.getMethod();
		boolean _result = isNotifyMethod(_sm);

		if (_result && method != null && cgi != null) {
			_result = wasMethodInvocationHelper(_sm, stmt, method, cgi);
		}
		return _result;
	}

	/**
	 * Checks if the given method is one of the <code>notify</code> methods in <code>java.lang.Object</code> class.
	 * 
	 * @param method to be checked.
	 * @return <code>true</code> if the method is <code>notify</code> methods in <code>java.lang.Object</code> class;
	 *         <code>false</code>, otherwise.
	 */
	@Functional public static boolean isNotifyMethod(@NonNull final SootMethod method) {
		return method.getDeclaringClass().getName().equals("java.lang.Object")
				&& (method.getName().equals("notify") || method.getName().equals("notifyAll"));
	}

	/**
	 * Checks if the given type is a valid reference type.
	 * 
	 * @param t is the type to checked.
	 * @return <code>true</code> if <code>t</code> is a valid reference type; <code>false</code>, otherwise.
	 */
	@Functional public static boolean isReferenceType(@NonNull final Type t) {
		return t instanceof RefType || t instanceof ArrayType || t instanceof NullType;
	}

	/**
	 * Checks if type <code>t1</code> is the same/sub-type of type <code>t2</code>.
	 * 
	 * @param t1 is the type to be checked for equivalence or sub typing.
	 * @param t2 is the type against which the check is conducted.
	 * @param env in which these types exists.
	 * @return <code>true</code> if <code>t1</code> is same as or sub type of <code>t2</code>; <code>false</code>,
	 *         otherwise.
	 * @post result == t1.oclIsKindOf(t2)
	 */
	@Immutable public static boolean isSameOrSubType(@NonNull @Immutable final Type t1, @NonNull @Immutable final Type t2,
			@NonNull final IEnvironment env) {
		boolean _result = false;

		if (t1.equals(t2)) {
			_result = true;
		} else if (t1 instanceof RefType && t2 instanceof RefType) {
			final SootClass _c1 = env.getClass(((RefType) t1).getClassName());
			final SootClass _c2 = env.getClass(((RefType) t2).getClassName());
			_result = isDescendentOf(_c1, _c2);
		}
		return _result;
	}

	/**
	 * Checks if the given method is <code>java.lang.Thread.start()</code> method.
	 * 
	 * @param method to be checked.
	 * @return <code>true</code> if the method is <code>java.lang.Thread.start()</code> method; <code>false</code>,
	 *         otherwise.
	 */
	@Functional public static boolean isStartMethod(@NonNull final SootMethod method) {
		return method.getName().equals("start") && method.getDeclaringClass().getName().equals("java.lang.Thread")
				&& method.getReturnType() instanceof VoidType && method.getParameterCount() == 0;
	}

	/**
	 * Checks if the method invoked at the invocation site is one of the <code>wait</code> methods in
	 * <code>java.lang.Object</code> class based on the given call graph.
	 * 
	 * @param stmt in which the invocation occurs.
	 * @param method in which <code>stmt</code> occurs.
	 * @param cgi to be used in the check.
	 * @return <code>true</code> if the method invoked at the invocation site is one of the <code>wait</code> methods in
	 *         <code>java.lang.Object</code> class; <code>false</code>, otherwise.
	 */
	@Immutable public static boolean isWaitInvocation(@NonNull @Immutable final InvokeStmt stmt,
			@Immutable final SootMethod method, @Immutable final ICallGraphInfo cgi) {
		final InvokeExpr _expr = stmt.getInvokeExpr();
		final SootMethod _sm = _expr.getMethod();
		boolean _result = isWaitMethod(_sm);

		if (_result && method != null && cgi != null) {
			_result = wasMethodInvocationHelper(_sm, stmt, method, cgi);
		}
		return _result;
	}

	/**
	 * Checks if the given method is one of the <code>wait</code> methods in <code>java.lang.Object</code> class.
	 * 
	 * @param method to be checked.
	 * @return <code>true</code> if the method is <code>wait</code> methods in <code>java.lang.Object</code> class;
	 *         <code>false</code>, otherwise.
	 */
	@Functional public static boolean isWaitMethod(@Immutable @NonNull final SootMethod method) {
		return method.getDeclaringClass().getName().equals("java.lang.Object") && method.getName().equals("wait");
	}

	/**
	 * Prunes the given list of traps that cover atleast one common statement.
	 * 
	 * @param enclosingTraps is the list of traps.
	 * @post enclosingTraps.containsAll(enclosingTraps$pre)
	 */
	public static void pruneEnclosingTraps(@NonNullContainer @NonNull final List<Trap> enclosingTraps) {
		final Collection<Trap> _r = new ArrayList<Trap>();
		int _size = enclosingTraps.size();
		for (int _i = 0; _i < _size; _i++) {
			final Trap _t1 = enclosingTraps.get(_i);
			final SootClass _c1 = _t1.getException();
			for (int _j = _i + 1; _j < _size; _j++) {
				final Trap _t2 = enclosingTraps.get(_j);
				final SootClass _c2 = _t2.getException();
				if (isDescendentOf(_c2, _c1)) {
					_r.add(_t2);
				}
			}
			enclosingTraps.removeAll(_r);
			_size -= _r.size();
			_r.clear();
		}
	}

	/**
	 * Removes methods from <code>methods</code> which have same signature as any methods in <code>methodsToRemove</code>.
	 * This is the counterpart of <code>retainMethodsWithSignature</code>.
	 * 
	 * @param methods is the collection of methods to be modified.
	 * @param methodsToRemove is the collection of methods to match signatures with those in <code>methods</code>.
	 */
	@Immutable public static void removeMethodsWithSameSignature(
			@NonNullContainer @NonNull final Collection<SootMethod> methods,
			@Immutable @NonNullContainer @NonNull final Collection<SootMethod> methodsToRemove) {
		final Collection<SootMethod> _removeSet = new HashSet<SootMethod>();
		_removeSet.addAll(methods);
		retainMethodsWithSameSignature(_removeSet, methodsToRemove);
		methods.removeAll(_removeSet);
	}

	/**
	 * Retains methods from <code>methods</code> which have same signature as any methods in <code>methodsToRemove</code>.
	 * This is the counterpart of <code>removeMethodsWithSignature</code>.
	 * 
	 * @param methods is the collection of methods to be modified.
	 * @param methodsToRetain is the collection of methods to match signatures with those in <code>methods</code>.
	 */
	@Immutable public static void retainMethodsWithSameSignature(
			@NonNullContainer @NonNull final Collection<SootMethod> methods,
			@NonNullContainer @NonNull @Immutable final Collection<SootMethod> methodsToRetain) {
		final Collection<SootMethod> _retainSet = new HashSet<SootMethod>();

		for (final Iterator<SootMethod> _j = methods.iterator(); _j.hasNext();) {
			final SootMethod _abstractMethod = _j.next();

			for (final Iterator<SootMethod> _k = methodsToRetain.iterator(); _k.hasNext();) {
				final SootMethod _method = _k.next();

				if (_abstractMethod.getSubSignature().equals(_method.getSubSignature())) {
					_retainSet.add(_abstractMethod);
				}
			}
		}
		methods.retainAll(_retainSet);
	}

	/**
	 * This is a helper method to check if <code>invokedMethod</code> is called at the site in the given statement and
	 * method in the given callgraph.
	 * 
	 * @param invokedMethod is the target method.
	 * @param stmt containing the invocation site.
	 * @param method containing <code>stmt</code>.
	 * @param cgi to be used for method resolution.
	 * @return <code>true</code> if <code> invokedMethod</code> is invoked; <code>false</code>, otherwise.
	 */
	@Functional private static boolean wasMethodInvocationHelper(@NonNull final SootMethod invokedMethod,
			@NonNull final InvokeStmt stmt, @NonNull final SootMethod method, @NonNull final ICallGraphInfo cgi) {
		final Context _context = new Context();
		_context.setRootMethod(method);
		_context.setStmt(stmt);

		boolean _result = false;
		final Collection<SootMethod> _callees = cgi.getCallees(stmt.getInvokeExpr(), _context);
		final Iterator<SootMethod> _iter = _callees.iterator();
		final int _iterEnd = _callees.size();

		for (int _iterIndex = 0; _iterIndex < _iterEnd && !_result; _iterIndex++) {
			final SootMethod _callee = _iter.next();
			_result |= _callee.equals(invokedMethod);
		}
		return _result;
	}
}

// End of File
