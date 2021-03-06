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

import edu.ksu.cis.indus.interfaces.IPrototype;
import edu.ksu.cis.indus.staticanalyses.tokens.ITokens;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.SootMethod;
import soot.Value;

/**
 * This class manages of method variants. This only provides the implementation to create new method variants. The super class
 * is responsible of managing the variants.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @version $Revision: 1.21 $
 * @param <SYM> is the type of symbol whose flow is being analyzed.
 * @param <T>  is the type of the token set object.
 * @param <N> is the type of the summary node in the flow analysis.
 */
class MethodVariantManager<SYM, T extends ITokens<T, SYM>, N extends IFGNode<SYM, T, N>, R>
		extends AbstractVariantManager<IMethodVariant<N>, SootMethod, SYM, T, N, R> {

	/**
	 * The logger used by instances of this class to log messages.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodVariantManager.class);

	/**
	 * A prototype object used to create index managers related to AST nodes. Objects created via this prototype object are
	 * used by <code>MethodVariant</code>s to manage the variants corresponding to the AST nodes that exists in them.
	 */
	protected final IPrototype<? extends IIndexManager<? extends IIndex<?>, Value>> astIMPrototype;

	/**
	 * The factory used to create method variants.
	 */
	private final IMethodVariantFactory<SYM, T, N, R> mvFactory;

	/**
	 * Creates a new <code>MethodVariantManager</code> instance.
	 *
	 * @param theAnalysis the instance of the framework in which this object is used. This parameter cannot be
	 *            <code>null</code>.
	 * @param indexManager the manager to indices which are used to map methods to their variants. This parameter cannot be
	 *            <code>null</code>.
	 * @param astIndexManagerPrototype the prototype object used to create index managers related to AST nodes. This
	 *            implementation should support <code>getClone()</code>.
	 * @param factory used to create method variants.
	 * @pre theAnalysis != null and indexManager != null and astIndexManagerPrototype != null
	 */
	MethodVariantManager(final FA<SYM, T, N, R> theAnalysis,
			final IIndexManager<? extends IIndex<?>, SootMethod> indexManager,
			final IPrototype<? extends IIndexManager<? extends IIndex<?>, Value>> astIndexManagerPrototype,
			final IMethodVariantFactory<SYM, T, N, R> factory) {
		super(theAnalysis, indexManager);
		this.astIMPrototype = astIndexManagerPrototype;
		mvFactory = factory;
	}

	/**
	 * Returns a new variant of the method represented by <code>o</code>.
	 *
	 * @param o the method whose variant is to be returned.
	 * @return the new <code>MethodVariant</code> corresponding to method <code>o</code>.
	 * @pre o != null and o.oclIsKindOf(SootMethod)
	 * @post result != null
	 */
	@Override protected IMethodVariant<N> getNewVariant(final SootMethod o) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("STATS: Processing method: " + o + "\t number: " + (getVariantCount() + 1));
		}

		final ASTVariantManager<SYM, T, N, R> _astVM = new ASTVariantManager<SYM, T, N, R>(fa, astIMPrototype.getClone());
		final SootMethod _sootMethod = o;
		return mvFactory.create(_sootMethod, _astVM, fa);
	}
}

// End of File
