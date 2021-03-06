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

package edu.ksu.cis.indus.interfaces;

import edu.ksu.cis.indus.common.datastructures.Triple;

import java.util.Collection;

import soot.Local;
import soot.SootClass;
import soot.SootMethod;
import soot.Value;

import soot.jimple.InvokeStmt;
import soot.jimple.MonitorStmt;

/**
 * The interface to access escape analysis information.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.20 $
 */
public interface IEscapeInfo
		extends IIdentification, IStatus {

	/**
	 * This is the unique identifier that can be used to identify an instance of this class.
	 */
	Comparable<String> ID = "Escape Information";

	/**
	 * This identifies shared access due to write by different threads.
	 */
	String WRITE_WRITE_SHARED_ACCESS = "write write shared access";

	/**
	 * This identifies shared access due to read by one thread and write by another thread.
	 */
	String READ_WRITE_SHARED_ACCESS = "read write shared access";

	/**
	 * Retrieves abstract threads which read the given local to read the fields of the referred object.
	 *
	 * @param local of interest.
	 * @param sm of interest.
	 * @return a collection of abstract thread objects. The implementation will have to explicitly specify the domain of these
	 *         objects.
	 * @pre sm != null and local != null
	 * @post result != null
	 */
	Collection<Triple<InvokeStmt, SootMethod, SootClass>> getReadingThreadsOf(Local local, SootMethod sm);

	/**
	 * Retrieves abstract threads which read the given parameter to read the fields of the referred object.
	 *
	 * @param paramIndex of interest.
	 * @param sm of interest.
	 * @return a collection of abstract thread objects. The implementation will have to explicitly specify the domain of these
	 *         objects.
	 * @pre sm != null and paramIndex >= 0
	 * @post result != null
	 */
	Collection<Triple<InvokeStmt, SootMethod, SootClass>> getReadingThreadsOf(int paramIndex, SootMethod sm);

	/**
	 * Retrieves abstract threads which read the "this" variable of the given method to read the fields of the referred
	 * object.
	 *
	 * @param sm of interest.
	 * @return a collection of abstract thread objects. The implementation will have to explicitly specify the domain of these
	 *         objects.
	 * @pre sm != null
	 * @post result != null
	 */
	Collection<Triple<InvokeStmt, SootMethod, SootClass>> getReadingThreadsOfThis(SootMethod sm);

	/**
	 * Retrieves abstract threads which read the given local in the given method to write to the fields of the referred
	 * object.
	 *
	 * @param local of interest.
	 * @param sm of interest.
	 * @return a collection of abstract thread objects. The implementation will have to explicitly specify the domain of these
	 *         objects.
	 * @pre sm != null and local != null
	 * @post result != null
	 */
	Collection<Triple<InvokeStmt, SootMethod, SootClass>> getWritingThreadsOf(Local local, SootMethod sm);

	/**
	 * Retrieves abstract threads which read the given parameter of the given method to write to the fields of the referred
	 * object.
	 *
	 * @param paramIndex of interest.
	 * @param sm of interest.
	 * @return a collection of abstract thread objects. The implementation will have to explicitly specify the domain of these
	 *         objects.
	 * @pre sm != null and paramIndex >= 0
	 * @post result != null
	 */
	Collection<Triple<InvokeStmt, SootMethod, SootClass>> getWritingThreadsOf(int paramIndex, SootMethod sm);

	/**
	 * Retrieves abstract threads which read the "this" variable of the given method to write to the fields of the referred
	 * object.
	 *
	 * @param sm of interest.
	 * @return a collection of abstract thread objects. The implementation will have to explicitly specify the domain of these
	 *         objects.
	 * @pre sm != null
	 * @post result != null
	 */
	Collection<Triple<InvokeStmt, SootMethod, SootClass>> getWritingThreadsOfThis(SootMethod sm);

	/**
	 * Checks if the given locals may point to some common object and that object may be locked.
	 *
	 * @param local1 is a variable whose object is used to realize a monitor. This should be <code>null</code> when querying
	 *            about the lock acquired/released while entering/exiting a synchronized method.
	 * @param method1 contains <code>local1</code>.
	 * @param local2 is another variable whose object is used to realize a monitor. This should be <code>null</code> when
	 *            querying about the lock acquired/released while entering/exiting a synchronized method.
	 * @param method2 contains <code>local2</code>.
	 * @return <code>true</code>if they may be coupled; <code>false</code>, otherwise.
	 * @pre method1 != null and method2 != null
	 */
	boolean areCoupledViaLocking(Local local1, SootMethod method1, Local local2, SootMethod method2);

	/**
	 * Checks if the given monitor statements are coupled (if they will operate on the same object).
	 *
	 * @param stmt1 is a monitor statement. This should be <code>null</code> when querying about the lock acquired/released
	 *            while entering/exiting a synchronized method.
	 * @param method1 contains <code>stmt1</code>.
	 * @param stmt2 is another monitor statement. This should be <code>null</code> when querying about the lock
	 *            acquired/released while entering/exiting a synchronized method.
	 * @param method2 contains <code>stmt2</code>.
	 * @return <code>true</code>if they may be coupled; <code>false</code>, otherwise.
	 * @pre method1 != null and method2 != null
	 */
	boolean areMonitorsCoupled(MonitorStmt stmt1, SootMethod method1, MonitorStmt stmt2, SootMethod method2);

	/**
	 * Checks if the given statement containing a <code>wait</code> invocation is coupled to the given statement containing
	 * <code>notify/All</code> invocation. By coupling we mean that the notification via the given notify invocation may
	 * reach the given wait invocation.
	 *
	 * @param wait is the statement containing <code>wait</code> invocation.
	 * @param waitMethod is the method in which <code>wait</code> occurs.
	 * @param notify is the statement containing <code>notify/All</code> invocation.
	 * @param notifyMethod is the method in which <code>notify</code> occurs.
	 * @return <code>true</code> if <code>wait</code> is ready dependent on <code>notify</code>; <code>false</code>,
	 *         otherwise.
	 * @pre wait != null and waitMethod != null and notify != null and notifyMethod != null
	 */
	boolean areWaitAndNotifyCoupled(InvokeStmt wait, SootMethod waitMethod, InvokeStmt notify, SootMethod notifyMethod);

	/**
	 * Checks if the object bound to the given program point in the given method escapes. This suggests mere multithread
	 * visibility and not multithread access.
	 *
	 * @param v is the program point being checked for escaping.
	 * @param sm is the method in which <code>v</code> occurs.
	 * @return <code>true</code> if <code>v</code> is escapes; <code>false</code>, otherwise.
	 * @pre v != null and sm != null
	 */
	boolean escapes(Value v, SootMethod sm);

	/**
	 * Checks if the given class in the given method escapes. This suggests mere multithread visibility and not multithread
	 * access. Although a class is accessible in all location it is visible at, it does not imply that it is accessed in
	 * multiple threads.
	 *
	 * @param sc is the class being checked for escaping.
	 * @param sm is the method in which <code>sc</code> occurs.
	 * @return <code>true</code> if <code>sc</code> is escapes; <code>false</code>, otherwise.
	 * @pre sc != null and sm != null
	 */
	boolean escapes(SootClass sc, SootMethod sm);

	/**
	 * Checks if the object bound to the given program point in the given method is shared. This suggests multithread field
	 * access.
	 *
	 * @param v is the program point being checked for sharing.
	 * @param sm is the method in which <code>v</code> occurs.
	 * @param sharedAccessSort has to either <code>READ_WRITE_SHARED_ACCESS</code> or <code>WRETE_WRITE_SHARED_ACCESS</code>.
	 * @return <code>true</code> if <code>v</code> is shared; <code>false</code>, otherwise.
	 * @pre v != null and sm != null
	 */
	boolean fieldAccessShared(Value v, SootMethod sm, Object sharedAccessSort);

	/**
	 * Checks if the object bound to the given program points are shared. This is more stricter than escape-ness. This
	 * requires that the values be escaping and be involved in a inter-thread field access operation.
	 *
	 * @param v1 is one of the program point in the check.
	 * @param sm1 is the method in which <code>v1</code> occurs.
	 * @param v2 is the other program point in the check.
	 * @param sm2 is the method in which <code>v2</code> occurs.
	 * @param sharedAccessSort has to either <code>READ_WRITE_SHARED_ACCESS</code> or <code>WRITE_WRITE_SHARED_ACCESS</code>.
	 * @return <code>true</code> if the given values are indeed shared across threads; <code>false</code>, otherwise.
	 * @pre v1 != null and sm1 != null and v2 != null and sm2 != null
	 */
	boolean fieldAccessShared(Value v1, SootMethod sm1, Value v2, SootMethod sm2, Object sharedAccessSort);

	/**
	 * Checks if the object bound to the given program point in the given method is shared via access to the specified field.
	 * This suggests multithread field access.
	 *
	 * @param v is the program point being checked for sharing.
	 * @param sm is the method in which <code>v</code> occurs.
	 * @param signature is the field signature of interest.
	 * @param sharedAccessSort has to either <code>READ_WRITE_SHARED_ACCESS</code> or <code>WRITE_WRITE_SHARED_ACCESS</code>.
	 * @return <code>true</code> if <code>v</code> is shared via access to the given field; <code>false</code>,
	 *         otherwise.
	 * @pre v != null and sm != null and signature != null
	 */
	boolean fieldAccessShared(Value v, SootMethod sm, String signature, Object sharedAccessSort);

	/**
	 * Checks if the object bound to the given program point in the given method shared. This suggests multithread lock-unlock
	 * access.
	 *
	 * @param v is the program point being checked for sharing.
	 * @param sm is the method in which <code>v</code> occurs.
	 * @return <code>true</code> if <code>v</code> is shared; <code>false</code>, otherwise.
	 * @pre v != null and sm != null
	 */
	boolean lockUnlockShared(Value v, SootMethod sm);

	/**
	 * Checks if the given class is shared in the given method via an access to a static field. This suggests multithread
	 * field access.
	 *
	 * @param sc is the class being checked for static field sharing.
	 * @param sm is the method in which <code>sc</code> is accessed for static field access.
	 * @param sharedAccessSort has to either <code>READ_WRITE_SHARED_ACCESS</code> or <code>READ_WRITE_SHARED_ACCESS</code>.
	 * @return <code>true</code> if <code>sc</code> is shared across threads for static field access; <code>false</code>,
	 *         otherwise.
	 * @pre sc != null and sm != null
	 */
	boolean staticfieldAccessShared(SootClass sc, SootMethod sm, Object sharedAccessSort);

	/**
	 * Checks if the given class is shared in the given method via an access to the static field with the given signature.
	 * This suggests multithread field access.
	 *
	 * @param sc is the class being checked for static field sharing.
	 * @param sm is the method in which <code>sc</code> is accessed for static field access.
	 * @param signature is the field signature of interest.
	 * @param sharedAccessSort has to either <code>READ_WRITE_SHARED_ACCESS</code> or <code>READ_WRITE_SHARED_ACCESS</code>.
	 * @return <code>true</code> if <code>sc</code> is shared across multiple threads for access to the given static
	 *         field; <code>false</code>, otherwise.
	 * @pre sc != null and sm != null and signature != null
	 */
	boolean staticfieldAccessShared(SootClass sc, SootMethod sm, String signature, Object sharedAccessSort);

	/**
	 * Checks if "this" variable of the given method escapes. If the method is static then the result is pessimistic, hence,
	 * <code>true</code> is returned.
	 *
	 * @param method in which "this" occurs.
	 * @return <code>true</code> if "this" escapes; <code>false</code>, otherwise.
	 * @pre method != null
	 */
	boolean thisEscapes(SootMethod method);

	/**
	 * Checks if "this" variable of the given method is shared via field access. If the method is static then the result is
	 * pessimistic, hence, <code>true</code> is returned.
	 *
	 * @param method in which "this" occurs.
	 * @param sharedAccessSort has to either <code>READ_WRITE_SHARED_ACCESS</code> or <code>READ_WRITE_SHARED_ACCESS</code>.
	 * @return <code>true</code> if "this" is shared; <code>false</code>, otherwise.
	 * @pre method != null
	 */
	boolean thisFieldAccessShared(SootMethod method, Object sharedAccessSort);

	/**
	 * Checks if "this" variable of the given method is shared via field access. If the method is static then the result is
	 * pessimistic, hence, <code>true</code> is returned.
	 *
	 * @param method in which "this" occurs. *
	 * @param signature is the field signature of interest.
	 * @param sharedAccessSort has to either <code>READ_WRITE_SHARED_ACCESS</code> or <code>READ_WRITE_SHARED_ACCESS</code>.
	 * @return <code>true</code> if "this" is shared; <code>false</code>, otherwise.
	 * @pre method != null
	 */
	boolean thisFieldAccessShared(SootMethod method, String signature, Object sharedAccessSort);

	/**
	 * Checks if "this" variable of the given method is lock-unlock shared. If the method is static then the result is
	 * pessimistic, hence, <code>true</code> is returned.
	 *
	 * @param method in which "this" occurs.
	 * @return <code>true</code> if "this" is shared; <code>false</code>, otherwise.
	 * @pre method != null
	 */
	boolean thisLockUnlockShared(SootMethod method);

	/**
	 * Checks if "this" variable of the given method is wait-notify shared. If the method is static then the result is
	 * pessimistic, hence, <code>true</code> is returned.
	 *
	 * @param method in which "this" occurs.
	 * @return <code>true</code> if "this" is shared; <code>false</code>, otherwise.
	 * @pre method != null
	 */
	boolean thisWaitNotifyShared(SootMethod method);

	/**
	 * Checks if the object bound to the given variable in the given method shared. This suggests multithread wait-notify
	 * access.
	 *
	 * @param v is the object value being checked for sharing.
	 * @param sm is the method in which <code>v</code> occurs.
	 * @return <code>true</code> if <code>v</code> is shared; <code>false</code>, otherwise.
	 * @pre v != null and sm != null
	 */
	boolean waitNotifyShared(Value v, SootMethod sm);
}

// End of File
