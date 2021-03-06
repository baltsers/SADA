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

package edu.ksu.cis.indus.common.scoping;

import edu.ksu.cis.indus.annotations.Empty;
import edu.ksu.cis.indus.annotations.Functional;
import edu.ksu.cis.indus.annotations.Immutable;
import edu.ksu.cis.indus.annotations.NonNull;
import edu.ksu.cis.indus.annotations.NonNullContainer;
import edu.ksu.cis.indus.interfaces.IEnvironment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.SootMethod;
import soot.Type;

/**
 * This class represents method-level scope specification.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.14 $ $Date: 2007/02/10 19:08:39 $
 */
public final class MethodSpecification
		extends AbstractSpecification {

	/**
	 * The logger used by instances of this class to log messages.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodSpecification.class);

	/**
	 * This is the specification of the type of the class that declares the method.
	 */
	@NonNull private TypeSpecification declaringClassSpec;

	/**
	 * The pattern of the method's name.
	 */
	@NonNull private Pattern namePattern;

	/**
	 * This is the specifications of the types of the parameters.
	 */
	@NonNullContainer private final List<TypeSpecification> parameterTypeSpecs = new ArrayList<TypeSpecification>();

	/**
	 * This is the specification of the return type of the method.
	 */
	@NonNull private TypeSpecification returnTypeSpec;

	/**
	 * Creates an instance of this class.
	 */
	@Empty public MethodSpecification() {
		super();
	}

	/**
	 * Creates the container for parameter type specifications. This is used by java-xml binding.
	 * 
	 * @return a container.
	 */
	@SuppressWarnings("unchecked") @NonNull static List createParameterTypeSpecContainer() {
		return new ArrayList();
	}

	/**
	 * Retrieves the specification of the class that declares the method.
	 * 
	 * @return the specification.
	 */
	@Functional @NonNull public TypeSpecification getDeclaringClassSpec() {
		return declaringClassSpec;
	}

	/**
	 * Retrieves the specification of the method's name.
	 * 
	 * @return the specification.
	 */
	@Functional @NonNull public String getMethodNameSpec() {
		return namePattern.pattern();
	}

	/**
	 * Retrieves the specification of the type of the parameters of the method.
	 * 
	 * @return a list of specifications.
	 */
	@Functional @NonNullContainer @NonNull public List<TypeSpecification> getParameterTypeSpecs() {
		return parameterTypeSpecs;
	}

	/**
	 * Retrieves the specification of the return type of the method.
	 * 
	 * @return the specification.
	 */
	@NonNull @Functional public TypeSpecification getReturnTypeSpec() {
		return returnTypeSpec;
	}

	/**
	 * Checks if the given method is in the scope of this specification in the given environment.
	 * 
	 * @param method to be checked for scope constraints.
	 * @param system in which the check the constraints.
	 * @return <code>true</code> if the given method lies within the scope defined by this specification; <code>false</code>,
	 *         otherwise.
	 */
	@Functional public boolean isInScope(@NonNull final SootMethod method, @NonNull final IEnvironment system) {
		boolean _result = namePattern.matcher(method.getName()).matches();
		_result = _result && declaringClassSpec.conformant(method.getDeclaringClass().getType(), system);
		_result = _result && returnTypeSpec.conformant(method.getReturnType(), system);
		_result = _result && accessConformant(new AccessSpecifierWrapper(method));

		if (_result) {
			@SuppressWarnings("unchecked") final List<Type> _parameterTypes = method.getParameterTypes();
			final Iterator<Type> _i = _parameterTypes.iterator();
			final int _iEnd = _parameterTypes.size();

			for (int _iIndex = 0; _iIndex < _iEnd && !_result; _iIndex++) {
				final Type _type = _i.next();
				final TypeSpecification _pTypeSpec = parameterTypeSpecs.get(_iIndex);

				if (_pTypeSpec != null) {
					_result |= _pTypeSpec.conformant(_type, system);
				}
			}
		}

		if (!isInclusion()) {
			_result = !_result;
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(this + " " + method + " " + _result);
		}

		return _result;
	}

	/**
	 * Sets the specification of the class that declares the method.
	 * 
	 * @param spec the specification.
	 */
	public void setDeclaringClassSpec(@NonNull @Immutable final TypeSpecification spec) {
		declaringClassSpec = spec;
	}

	/**
	 * Sets the specification of the method's name.
	 * 
	 * @param spec is a regular expression.
	 */
	public void setMethodNameSpec(@NonNull @Immutable final String spec) {
		namePattern = Pattern.compile(spec);
	}

	/**
	 * Sets the specification of the type of the parameters of the method.
	 * 
	 * @param specs the specifications.
	 */
	public void setParameterTypeSpecs(@NonNull @Immutable final List<TypeSpecification> specs) {
		parameterTypeSpecs.addAll(specs);
	}

	/**
	 * Sets the specification of the return type of the method.
	 * 
	 * @param spec the specification.
	 */
	public void setReturnTypeSpec(@NonNull @Immutable final TypeSpecification spec) {
		returnTypeSpec = spec;
	}

	/**
	 * {@inheritDoc}
	 */
	@Functional @NonNull @Override public String toString() {
		return new ToStringBuilder(this).appendSuper(super.toString()).append("namePattern", this.namePattern.pattern())
				.append("returnTypeSpec", this.returnTypeSpec).append("parameterTypeSpecs", this.parameterTypeSpecs).append(
						"declaringClassSpec", this.declaringClassSpec).toString();
	}
}

// End of File
