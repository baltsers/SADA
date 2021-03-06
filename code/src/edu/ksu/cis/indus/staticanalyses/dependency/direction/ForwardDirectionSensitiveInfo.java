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

package edu.ksu.cis.indus.staticanalyses.dependency.direction;

import edu.ksu.cis.indus.common.soot.BasicBlockGraph.BasicBlock;

import java.util.Collection;
import java.util.List;

import soot.jimple.Stmt;

/**
 * This class provides information to drive the analysis to generate information that is forward in direction.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.6 $ $Date: 2007/02/10 19:07:49 $
 */
public final class ForwardDirectionSensitiveInfo
		implements IDirectionSensitiveInfo {

	/**
	 * @see IDirectionSensitiveInfo#getFirstStmtInBB(edu.ksu.cis.indus.common.soot.BasicBlockGraph.BasicBlock)
	 */
	public Stmt getFirstStmtInBB(final BasicBlock bb) {
		return bb.getTrailerStmt();
	}

	/**
	 * @see IDirectionSensitiveInfo#getFollowersOfBB(edu.ksu.cis.indus.common.soot.BasicBlockGraph.BasicBlock)
	 */
	public Collection<BasicBlock> getFollowersOfBB(final BasicBlock bb) {
		return bb.getPredsOf();
	}

	/**
	 * {@inheritDoc} This provides information for forward direciton.
	 */
	public List<Stmt> getIntraBBDependents(final BasicBlock bb, final Stmt stmt) {
		final List<Stmt> _stmtsFrom = bb.getStmtsFromTo(bb.getLeaderStmt(), stmt);
		_stmtsFrom.remove(stmt);
		return _stmtsFrom;
	}
}

// End of File
