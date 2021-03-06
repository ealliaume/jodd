// Copyright (c) 2003-present, Jodd Team (jodd.org). All Rights Reserved.

package jodd.http;

import jodd.http.up.Uploadable;
import jodd.io.StreamUtil;
import jodd.util.StringPool;
import jodd.util.buffer.FastByteBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.LinkedList;

/**
 * Holds request/response content until it is actually send.
 * File content (i.e. {@link jodd.http.up.Uploadable}) is
 * <b>not</b> read until it is really used.
 */
public class Buffer {

	protected LinkedList<Object> list = new LinkedList<Object>();
	protected FastByteBuffer last;
	protected int size;

	/**
	 * Appends string content to buffer.
	 */
	public Buffer append(String string) {
		ensureLast();

		try {
			byte[] bytes = string.getBytes(StringPool.ISO_8859_1);

			last.append(bytes);

			size += bytes.length;
		} catch (UnsupportedEncodingException ignore) {
		}

		return this;
	}

	/**
	 * Appends a char.
	 */
	public Buffer append(char c) {
		append(Character.toString(c));
		return this;
	}

	/**
	 * Appends a number.
	 */
	public Buffer append(int number) {
		append(Integer.toString(number));
		return this;
	}

	/**
	 * Appends {@link jodd.http.up.Uploadable} to buffer.
	 */
	public Buffer append(Uploadable uploadable) {
		list.add(uploadable);
		size += uploadable.getSize();
		last = null;
		return this;
	}

	/**
	 * Appends other buffer to this one.
	 */
	public Buffer append(Buffer buffer) {
		if (buffer.list.size() == 0) {
			// nothing to append
			return buffer;
		}
		list.addAll(buffer.list);
		last = buffer.last;
		size += buffer.size;
		return this;
	}

	/**
	 * Returns buffer size.
	 */
	public int size() {
		return size;
	}

	/**
	 * Ensures that last buffer exist.
	 */
	private void ensureLast() {
		if (last == null) {
			last = new FastByteBuffer();
			list.add(last);
		}
	}

	// ---------------------------------------------------------------- write

	/**
	 * Writes content to the writer.
	 */
	public void writeTo(Writer writer) throws IOException {
		for (Object o : list) {
			if (o instanceof FastByteBuffer) {
				FastByteBuffer fastByteBuffer = (FastByteBuffer) o;

				byte[] array = fastByteBuffer.toArray();

				writer.write(new String(array, StringPool.ISO_8859_1));
			}
			else if (o instanceof Uploadable) {
				Uploadable uploadable = (Uploadable) o;

				InputStream inputStream = uploadable.openInputStream();

				try {
					StreamUtil.copy(inputStream, writer, StringPool.ISO_8859_1);
				}
				finally {
					StreamUtil.close(inputStream);
				}
			}
		}
	}

	/**
	 * Writes content to the output stream.
	 */
	public void writeTo(OutputStream out) throws IOException {
		for (Object o : list) {
			if (o instanceof FastByteBuffer) {
				FastByteBuffer fastByteBuffer = (FastByteBuffer) o;

				out.write(fastByteBuffer.toArray());
			}
			else if (o instanceof Uploadable) {
				Uploadable uploadable = (Uploadable) o;

				InputStream inputStream = uploadable.openInputStream();

				try {
					StreamUtil.copy(inputStream, out);
				}
				finally {
					StreamUtil.close(inputStream);
				}
			}
		}
	}

	/**
	 * Writes content to the output stream, using progress listener to track the sending progress.
	 */
	public void writeTo(OutputStream out, HttpProgressListener progressListener) throws IOException {

		// start

		final int size = size();
		final int callbackSize = progressListener.callbackSize(size);
		int count = 0;		// total count
		int step = 0;		// step is offset in current chunk

		progressListener.transferred(count);

		// loop

		for (Object o : list) {
			if (o instanceof FastByteBuffer) {
				FastByteBuffer fastByteBuffer = (FastByteBuffer) o;
				byte[] bytes = fastByteBuffer.toArray();

				int offset = 0;

				while (offset < bytes.length) {
					// calc the remaining sending chunk size
					int chunk = callbackSize - step;

					// check if this chunk size fits the bytes array
					if (offset + chunk > bytes.length) {
						chunk = bytes.length - offset;
					}

					// writes the chunk
					out.write(bytes, offset, chunk);

					offset += chunk;
					step += chunk;
					count += chunk;

					// listener
					if (step >= callbackSize) {
						progressListener.transferred(count);
						step -= callbackSize;
					}
				}
			}
			else if (o instanceof Uploadable) {
				Uploadable uploadable = (Uploadable) o;

				InputStream inputStream = uploadable.openInputStream();

				int remaining = uploadable.getSize();

				try {
					while (remaining > 0) {
						// calc the remaining sending chunk size
						int chunk = callbackSize - step;

						// check if this chunk size fits the remaining size
						if (chunk > remaining) {
							chunk = remaining;
						}

						// writes remaining chunk
						StreamUtil.copy(inputStream, out, chunk);

						remaining -= chunk;
						step += chunk;
						count += chunk;

						// listener
						if (step >= callbackSize) {
							progressListener.transferred(count);
							step -= callbackSize;
						}
					}
				}
				finally {
					StreamUtil.close(inputStream);
				}
			}
		}

		// end

		if (step != 0) {
			progressListener.transferred(count);
		}
	}

}