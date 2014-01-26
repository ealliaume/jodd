package jodd.madvoc.result;

import jodd.util.ReflectUtil;
import jodd.util.StringPool;

/**
 * Base implementation of {@link jodd.madvoc.result.ActionResult Action result}.
 */
public abstract class BaseActionResult<T> implements ActionResult<T> {

	protected final String resultType;
	protected final Class<T> resultValueType;
	protected boolean initialized;

	/**
	 * Creates new action result that has a string identification.
	 */
	protected BaseActionResult(String resultType) {
		this.resultType = resultType;
		this.resultValueType = ReflectUtil.getGenericSupertype(this.getClass());
	}

	/**
	 * Creates new action result without a string identification.
	 */
	protected BaseActionResult() {
		this.resultType = null;
		this.resultValueType = ReflectUtil.getGenericSupertype(this.getClass());
	}

	/**
	 * {@inheritDoc}
	 */
	public String getResultType() {
		return resultType;
	}

	/**
	 * {@inheritDoc}
	 */
	public Class<T> getResultValueType() {
		return resultValueType;
	}

	/**
	 * Returns <code>true</code> if interceptor is initialized.
	 */
	public final boolean isInitialized() {
	    return initialized;
	}

	/**
	 * Initializes the result.
	 */
	public void init() {
		initialized = true;
	}

	@Override
	public String toString() {
		return "result: " + getClass().getSimpleName() +
				(resultType != null ? StringPool.COLON + resultType : StringPool.EMPTY);
	}

}